package com.dulno.team.access.cluster;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.error.ErrorRepository;
import com.dulno.team.access.TeamController;
import com.dulno.team.cluster.ClusterConfiguration;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.jsonwebtoken.Jwts;
import io.kubernetes.client.Copy;
import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@RestController
public final class ClusterBackupController extends TeamController {
  private final ErrorRepository errorRepository;
  private final CoreV1Api kubernetesCoreApi;
  private final Exec kubernetesExecutionApi;
  private final Copy kubernetesCopyApi;
  private final ClusterConfiguration clusterConfiguration;

  private ClusterBackupController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, ErrorRepository errorRepository,
    CoreV1Api kubernetesCoreApi, Exec kubernetesExecutionApi,
    Copy kubernetesCopyApi, ClusterConfiguration clusterConfiguration
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.errorRepository = errorRepository;
    this.kubernetesCoreApi = kubernetesCoreApi;
    this.kubernetesExecutionApi = kubernetesExecutionApi;
    this.kubernetesCopyApi = kubernetesCopyApi;
    this.clusterConfiguration = clusterConfiguration;
  }

  @RequestMapping(path = "/cluster/backups/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findClusterBackups(
    HttpServletRequest request
  ) {
    return memberHasPermission(findMemberId(request), "cluster.backups")
      .thenApplyAsync(this::findClusterBackups);
  }

  private Map<String, Object> findClusterBackups(
    boolean hasPermission
  ) {
    if (!hasPermission) {
      return Maps.newHashMap();
    }
    var backups = Maps.<Long, List<Map<String, Object>>>newHashMap();
    for (var pod : fetchDatabasePods()) {
      var podBackups = fetchDatabaseBackups(pod).stream()
        .map(line -> line.replaceAll(".*_(\\d+)_(\\d+)\\.tar\\.gz", "$2"))
        .map(Long::parseLong).toList();
      for (var backup : podBackups) {
        var origin = Map.<String, Object>of("pod", pod.getMetadata().getName(),
          "node", pod.getSpec().getNodeName(), "ip", pod.getStatus().getPodIP());
        if (backups.containsKey(backup)) {
          var entry = backups.get(backup);
          entry.add(origin);
          backups.put(backup, entry);
        } else {
          backups.put(backup, Lists.newArrayList(origin));
        }
      }
    }
    return Map.of("backups", backups);
  }

  @RequestMapping(path = "/cluster/backup/download/", method = RequestMethod.POST)
  public CompletableFuture<Void> downloadClusterBackup(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    return memberHasPermission(findMemberId(request), "cluster.backup.download")
      .thenAcceptAsync(hasPermission -> downloadClusterBackup(
        body.getLong("backup"), hasPermission, response));
  }

  private void downloadClusterBackup(
    long backupId, boolean hasPermission, HttpServletResponse response
  ) {
    if (!hasPermission) {
      return;
    }
    var directory = new File(System.getProperty("user.dir") + "/backups/");
    directory.mkdirs();
    for (var pod : fetchDatabasePods()) {
      downloadBackupFromPod(backupId, pod, directory);
    }
    var result = encryptBackups(directory);
    provideClusterBackup(result, response);
    try {
      FileUtils.deleteDirectory(directory);
      result.delete();
    } catch (Exception exception) {
      errorRepository.processError(exception);
    }
  }

  private void downloadBackupFromPod(long backupId, V1Pod pod, File parent) {
    var backup = fetchDatabaseBackups(pod).stream()
      .filter(line -> Long.parseLong(
        line.replaceAll(".*_(\\d+)_(\\d+)\\.tar\\.gz", "$2")) == backupId)
      .findFirst();
    if (backup.isEmpty()) {
      return;
    }
    var remotePath = clusterConfiguration.databasePath() + "/" + backup.get();
    var file = new File(parent, backup.get());
    try {
      try (var inputStream = kubernetesCopyApi.copyFileFromPod(pod, remotePath)) {
        Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (Exception exception) {
      errorRepository.processError(exception);
      file.delete();
    }
  }

  private void provideClusterBackup(File result, HttpServletResponse response) {
    try {
      var inputStream = new FileInputStream(result);
      var out = response.getOutputStream();
      var buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
      }
      out.flush();
      inputStream.close();
    } catch (Exception exception) {
      errorRepository.processError(exception);
    }
  }

  private File encryptBackups(File backups) {
    try {
      Runtime.getRuntime().exec("tar -cvzf " + System.getProperty("user.dir") +
        "/backups.tar.gz " + backups.getPath()).waitFor();
      var compressed = new File(System.getProperty("user.dir") + "/backups.tar.gz");
      var encrypted = new File(System.getProperty("user.dir") + "/backups.tar.gz.gpg");
      encryptFile(compressed.getPath(), encrypted.getPath(),
        clusterConfiguration.backupEncryptionKeyPath(),
        clusterConfiguration.backupEncryptionKeyName());
      compressed.delete();
      return encrypted;
    } catch (Exception exception) {
      errorRepository.processError(exception);
      return null;
    }
  }

  private void encryptFile(
    String inputPath, String outputPath, String publicKeyPath,
    String publicKeyName
  ) throws Exception {
    String[] commands = {"bash", "-c", "gpg --quiet --import " + publicKeyPath +
      " && KEY_FINGERPRINT=$(gpg --list-keys --with-colons | grep '^fpr' | " +
      "head -n 1 | cut -d':' -f10) && echo \"$KEY_FINGERPRINT:6:\" | " +
      "gpg --quiet --import-ownertrust && gpg --quiet --batch --output " +
      outputPath + " --encrypt --recipient '" + publicKeyName + "' " + inputPath};
    new ProcessBuilder(commands).start().waitFor();
  }

  @RequestMapping(path = "/cluster/backup/upload/", method = RequestMethod.POST)
  public CompletableFuture<Void> uploadClusterBackup(
    HttpServletRequest request
  ) {
    return memberHasPermission(findMemberId(request), "cluster.backup.upload")
      .thenAcceptAsync(hasPermission -> uploadClusterBackup(request, hasPermission));
  }

  private void uploadClusterBackup(
    HttpServletRequest request, boolean hasPermission
  ) {
    if (!hasPermission) {
      return;
    }
    var encrypted = new File(System.getProperty("user.dir") + "/backups.tar.gz.gpg");
    try {
      Files.copy(request.getPart("file").getInputStream(), encrypted.toPath(),
        StandardCopyOption.REPLACE_EXISTING);
      var backups = decryptFile(encrypted);
      for (var pod : fetchDatabasePods()) {
        var backup = Arrays.stream(backups.listFiles()).filter(entry ->
          entry.getName().contains(pod.getMetadata().getName())).findFirst();
        backup.ifPresent(entry -> uploadBackupToPod(entry, pod));
      }
      FileUtils.deleteDirectory(backups);
    } catch (Exception exception) {
      errorRepository.processError(exception);
      encrypted.delete();
    }
  }

  private void uploadBackupToPod(File backup, V1Pod pod) {
    var remotePath = Path.of(clusterConfiguration.databasePath() + "/" +
      backup.getName());
    try {
      var command = new String[]{"sh", "-c", "cat > " + remotePath};
      var targetProcess = kubernetesExecutionApi.exec(pod, command, true);
      try (var inputStream = new FileInputStream(backup);
           var targetStream = targetProcess.getOutputStream()
      ) {
        inputStream.transferTo(targetStream);
      }
      targetProcess.destroy();
    } catch (Exception exception) {
      errorRepository.processError(exception);
    }
  }

  private File decryptFile(File encrypted) {
    var compressed = new File(System.getProperty("user.dir") + "/backups.tar.gz");
    try {
      decryptFile(encrypted.getPath(), compressed.getPath(),
        clusterConfiguration.backupDecryptionKeyPath(),
        clusterConfiguration.backupDecryptionKeyName());
      Runtime.getRuntime().exec("tar -xvf " + compressed.getPath()).waitFor();
      compressed.delete();
      encrypted.delete();
      return new File(System.getProperty("user.dir") + "/backups");
    } catch (Exception exception) {
      errorRepository.processError(exception);
      compressed.delete();
      return null;
    }
  }

  private void decryptFile(
    String inputPath, String outputPath, String privateKeyPath,
    String privateKeyName
  ) throws Exception {
    var password = System.getenv("CASSANDRA_BACKUP_PRIVATE_KEY_PASSWORD");
    String[] commands = {"bash", "-c", "gpg --quiet --import " +
      "--pinentry-mode=loopback --passphrase '" + password + "' " + privateKeyPath +
      " && KEY_FINGERPRINT=$(gpg --list-keys --with-colons | grep '^fpr' | " +
      "head -n 1 | cut -d':' -f10) && echo \"$KEY_FINGERPRINT:6:\" | " +
      "gpg --quiet --import-ownertrust && gpg -o " + outputPath +
      " --pinentry-mode=loopback --passphrase '" + password + "' --recipient '" +
      privateKeyName + "' " + inputPath};
    new ProcessBuilder(commands).start().waitFor();
  }

  @RequestMapping(path = "/cluster/backup/restore/", method = RequestMethod.GET)
  public SseEmitter restoreClusterBackup(
    @RequestParam("token") String token, @RequestParam("backup") long backup,
    @RequestParam("keyspace") String keyspaceFilter,
    @RequestParam("table") String tableFilter,
    @RequestParam("keepMaintenance") boolean keepMaintenance
    ) {
    var response = ((ServletRequestAttributes)
      RequestContextHolder.currentRequestAttributes()).getResponse();
    if (response != null) {
      response.setHeader("Cache-Control", "no-store");
      response.setHeader("Connection", "keep-alive");
      response.setHeader("X-Accel-Buffering", "no");
    }
    var emitter = new SseEmitter(-1L);
    var memberId = findMemberIdSecured(token);
    if (memberId.isEmpty()) {
      emitter.complete();
      return emitter;
    }
    memberHasPermission(memberId.get(), "cluster.backup.restore")
      .thenAcceptAsync(hasPermission -> restoreClusterBackup(backup,
        keyspaceFilter, tableFilter, keepMaintenance, hasPermission, emitter));
    return emitter;
  }

  private void restoreClusterBackup(
    long backupId, String keyspaceFilter, String tableFilter,
    boolean keepMaintenance, boolean hasPermission, SseEmitter emitter
  ) {
    if (!hasPermission) {
      emitter.complete();
      return;
    }
    var pods = fetchDatabasePods();
    var completedPods = new AtomicInteger(0);
    var totalPods = new AtomicInteger(pods.size());
    var processes = Lists.<Runnable>newArrayList();
    for (var pod : pods) {
      processes.addAll(restoreBackupFromPod(backupId, keyspaceFilter, tableFilter,
        keepMaintenance, pod, emitter, completedPods, totalPods));
    }
    Collections.shuffle(processes);
    var executorService = Executors.newFixedThreadPool(10);
    for (var process : processes) {
      executorService.submit(process);
    }
  }

  private List<Runnable> restoreBackupFromPod(
    long backupId, String keyspaceFilter, String tableFilter,
    boolean keepMaintenance, V1Pod pod, SseEmitter emitter,
    AtomicInteger completedPods, AtomicInteger podCount
  ) {
    var backup = fetchDatabaseBackups(pod).stream()
      .filter(line -> Long.parseLong(
        line.replaceAll(".*_(\\d+)_(\\d+)\\.tar\\.gz", "$2")) == backupId)
      .findFirst();
    if (backup.isEmpty()) {
      podCount.decrementAndGet();
      return Lists.newArrayList();
    }
    var backupPodId = backup.get().split("_")[2];
    prepareBackupPod(pod, backup.get());
    var keyspaces = filterRestoreKeyspaces(listFilesInPod(pod,
      clusterConfiguration.databasePath(), line ->
        !line.contains("system") && !line.contains("snapshot")), keyspaceFilter);
    return restoreKeyspacesBackup(tableFilter, keepMaintenance,
      pod, backupPodId, keyspaces, emitter, completedPods, podCount);
  }

  private void prepareBackupPod(V1Pod pod, String backup) {
    var remotePath = clusterConfiguration.databasePath() + "/" + backup;
    try {
      kubernetesExecutionApi.exec(pod, new String[] {"tar", "-xvf", remotePath},
        true).destroy();
      kubernetesExecutionApi.exec(pod, new String[] {"nodetool", "flush"}, true)
        .waitFor();
    } catch (Exception exception) {
      errorRepository.processError(exception);
    }
  }

  private List<Runnable> restoreKeyspacesBackup(
    String tableFilter, boolean keepMaintenance, V1Pod pod,
    String backupPodId, List<String> keyspaces, SseEmitter emitter,
    AtomicInteger completedPods, AtomicInteger podCount
  ) {
    var completedTables = new AtomicInteger(0);
    var tableCount = new AtomicInteger(0);
    var processes = Lists.<Runnable>newArrayList();
    for (var keyspace : keyspaces) {
      processes.addAll(restoreTablesBackup(tableFilter, keepMaintenance, pod,
        backupPodId, emitter, completedPods, podCount, keyspace, completedTables,
        tableCount));
    }
    sendEmitterMessage(emitter, Map.of("type", "POD", "tableCount",
      tableCount.get(), "pod", pod.getMetadata().getName(), "podCount",
      podCount.get()));
    return processes;
  }

  private List<Runnable> restoreTablesBackup(
    String tableFilter, boolean keepMaintenance, V1Pod pod,
    String backupPodId, SseEmitter emitter, AtomicInteger completedPods,
    AtomicInteger podCount, String keyspace, AtomicInteger completedTables,
    AtomicInteger tableCount
  ) {
    var processes = Lists.<Runnable>newArrayList();
    var tables = filterRestoreTables(
      listFilesInPod(pod, clusterConfiguration.databasePath() + "/" + keyspace),
      tableFilter, keepMaintenance);
    for (var table : tables) {
      processes.add(() -> restoreTableBackup(pod, keyspace, table, backupPodId,
        emitter, completedTables, tableCount, completedPods, podCount));
      tableCount.incrementAndGet();
    }
    return processes;
  }

  private List<String> filterRestoreKeyspaces(
    List<String> keyspaces, String keyspaceFilter
  ) {
    return keyspaces.stream()
      .filter(keyspace -> keyspaceFilter.isEmpty() ||
        checkRestoreFilter(keyspaceFilter, keyspace))
      .toList();
  }

  private List<String> filterRestoreTables(
    List<String> tables, String tableFilter, boolean keepMaintenance
  ) {
    return tables.stream()
      .filter(table -> tableFilter.isEmpty() ||
        checkRestoreFilter(tableFilter, table))
      .filter(table -> !keepMaintenance || !table.contains("maintenance"))
      .toList();
  }

  private boolean checkRestoreFilter(String filter, String entry) {
    var sections = filter.replaceAll(" ", "").split(",");
    for (var section : sections) {
      if (entry.contains(section)) {
        return true;
      }
    }
    return false;
  }

  private void restoreTableBackup(
    V1Pod pod, String keyspace, String table,
    String backupPodId, SseEmitter emitter, AtomicInteger completedTables,
    AtomicInteger tableCount, AtomicInteger completedPods, AtomicInteger podCount
  ) {
    copyTableBackup(pod, keyspace, table, backupPodId);
    sendEmitterMessage(emitter, Map.of("type", "TABLE", "keyspace", keyspace,
      "table", table, "pod", pod.getMetadata().getName()));
    if (completedTables.incrementAndGet() == tableCount.get()) {
      if (completedPods.incrementAndGet() == podCount.get()) {
        sendEmitterMessage(emitter, Map.of("type", "COPIED"));
        restartDatabase(emitter);
      }
    }
  }

  private void copyTableBackup(
    V1Pod pod, String keyspace, String table, String backupPodId
  ) {
    try {
      kubernetesExecutionApi.exec(pod, new String[] {"bash", "-c",
          "find " + clusterConfiguration.databasePath() + "/" + keyspace + "/" +
            table + " -mindepth 1 -maxdepth 1 ! -name '*snapshot*' -exec rm -rf {} +"},
        true, true).waitFor();
      kubernetesExecutionApi.exec(pod, new String[] {
        "cp", "-a", "--remove-destination",
          clusterConfiguration.databasePath() + "/" + keyspace + "/" + table +
            "/snapshots/" + backupPodId + "/.",
          clusterConfiguration.databasePath() + "/" + keyspace + "/" + table + "/"},
        true, true).waitFor();
    } catch (Exception exception) {
      errorRepository.processError(exception);
    }
  }

  private void restartDatabase(SseEmitter emitter) {
    var pods = fetchDatabasePods();
    if (pods.isEmpty()) {
      return;
    }
    var restartedPods = new AtomicInteger(0);
    for (var i = 0; i < pods.size(); i++) {
      var pod = pods.get(i);
      var nextPods = i == pods.size() - 1 ? Lists.<String>newArrayList() :
        Lists.newArrayList(pods.get(i + 1).getMetadata().getName());
      try {
        waitPodRestart(pod, emitter, restartedPods, pods.size(), nextPods);
      } catch (Exception exception) {
        errorRepository.processError(exception);
      }
    }
  }

  private void waitPodRestart(
    V1Pod pod, SseEmitter emitter, AtomicInteger restartedPods,
    int totalPodNumber, List<String> nextPods
  ) throws Exception {
    kubernetesCoreApi.deleteNamespacedPod(pod.getMetadata().getName(),
      pod.getMetadata().getNamespace()).execute();
    var endTime = Instant.now().plus(Duration.ofMinutes(15));
    while (Instant.now().isBefore(endTime)) {
      Thread.sleep(2000);
      if (checkPodRestart(pod, emitter,  restartedPods, totalPodNumber, nextPods)) {
        break;
      }
    }
  }

  private boolean checkPodRestart(
    V1Pod pod, SseEmitter emitter, AtomicInteger restartedPods,
    int totalPodNumber, List<String> nextPods
  ) throws Exception {
    sendEmitterMessage(emitter, Map.of("type", "PING"));
    if (isPodReady(pod)) {
      sendEmitterMessage(emitter, Map.of("type", "RESTART",
        "pod", pod.getMetadata().getName(), "nextPods", nextPods));
      if (restartedPods.incrementAndGet() == totalPodNumber) {
        sendEmitterMessage(emitter, Map.of("type", "RESTARTED"));
        emitter.complete();
      }
      return true;
    }
    return false;
  }

  private boolean isPodReady(V1Pod pod) throws Exception {
    var updatedPod = kubernetesCoreApi.readNamespacedPod(
      pod.getMetadata().getName(), pod.getMetadata().getNamespace()).execute();
    if (updatedPod.getStatus().getStartTime().toEpochSecond() ==
      pod.getStatus().getStartTime().toEpochSecond()
    ) {
      return false;
    }
    var conditions = updatedPod.getStatus().getConditions();
    if (!"Running".equals(updatedPod.getStatus().getPhase()) || conditions == null) {
      return false;
    }
    return conditions.stream().anyMatch(condition ->
      "Ready".equals(condition.getType()) && "True".equals(condition.getStatus()));
  }

  private void sendEmitterMessage(SseEmitter emitter, Map<String, Object> content) {
    try {
      emitter.send(new JSONObject(content).toString());
    } catch (Exception ignored) {
    }
  }

  @RequestMapping(path = "/cluster/backup/delete/", method = RequestMethod.POST)
  public CompletableFuture<Void> deleteClusterBackup(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    return memberHasPermission(findMemberId(request), "cluster.backup.delete")
      .thenAcceptAsync(hasPermission -> deleteClusterBackup(
        body.getLong("backup"), hasPermission));
  }

  private void deleteClusterBackup(long backupId, boolean hasPermission) {
    if (!hasPermission) {
      return;
    }
    for (var pod : fetchDatabasePods()) {
      deleteBackupFromPod(backupId, pod);
    }
  }

  private void deleteBackupFromPod(long backupId, V1Pod pod) {
    var backup = fetchDatabaseBackups(pod).stream()
      .filter(line -> Long.parseLong(
        line.replaceAll(".*_(\\d+)_(\\d+)\\.tar\\.gz", "$2")) == backupId)
      .findFirst();
    if (backup.isEmpty()) {
      return;
    }
    var remotePath = clusterConfiguration.databasePath() + "/" + backup.get();
    try {
      kubernetesExecutionApi.exec(pod, new String[] {"rm", remotePath},
        true, true).waitFor();
    } catch (Exception exception) {
      errorRepository.processError(exception);
    }
  }

  private List<V1Pod> fetchDatabasePods() {
    try {
      return kubernetesCoreApi.listNamespacedPod(clusterConfiguration.namespace())
        .execute().getItems().stream()
        .filter(pod -> pod.getMetadata().getOwnerReferences().stream()
          .anyMatch(owner -> owner.getName()
            .equals(clusterConfiguration.databaseController())))
        .toList();
    } catch (Exception exception) {
      errorRepository.processError(exception);
      return Lists.newArrayList();
    }
  }

  private List<String> fetchDatabaseBackups(V1Pod databasePod) {
    return listFilesInPod(databasePod, clusterConfiguration.databasePath(),
      line -> line.contains("snapshot"));
  }

  private List<String> listFilesInPod(V1Pod pod, String path) {
    return listFilesInPod(pod, path, line -> true);
  }

  private List<String> listFilesInPod(
    V1Pod pod, String path, Function<String, Boolean> filter
  ) {
    try {
      var process = kubernetesExecutionApi.exec(pod, new String[] {"ls", path},
        true, true);
      var backups = Lists.<String>newArrayList();
      try (var reader = new BufferedReader(new InputStreamReader(
        process.getInputStream())
      )) {
        backups.addAll(reader.lines().filter(filter::apply).toList());
      }
      process.waitFor();
      return backups;
    } catch (Exception exception) {
      errorRepository.processError(exception);
      return Lists.newArrayList();
    }
  }

  private Optional<UUID> findMemberIdSecured(String token) {
    try {
      return Optional.of(UUID.fromString(Jwts.parser().setSigningKey(secretKey())
        .build().parseClaimsJws(token).getPayload().get("id", String.class)));
    } catch (Exception exception) {
      return Optional.empty();
    }
  }
}
