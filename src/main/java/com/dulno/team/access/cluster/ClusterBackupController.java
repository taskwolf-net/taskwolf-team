package com.dulno.team.access.cluster;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.error.ErrorRepository;
import com.dulno.team.access.TeamController;
import com.dulno.team.cluster.ClusterConfiguration;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.kubernetes.client.Copy;
import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
      copyBackupFromPod(backupId, pod, directory);
    }
    var result = compressBackups(directory);
    provideClusterBackup(result, response);
    directory.delete();
    result.delete();
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

  private File compressBackups(File backups) {
    try {
      Runtime.getRuntime().exec("tar -cvzf " + System.getProperty("user.dir") +
        "/backups.tar.gz " + backups.getPath()).waitFor();
      return new File(System.getProperty("user.dir") + "/backups.tar.gz");
    } catch (Exception exception) {
      errorRepository.processError(exception);
      return null;
    }
  }

  private void copyBackupFromPod(long backupId, V1Pod pod, File parent) {
    var backup = fetchDatabaseBackups(pod).stream()
      .filter(line -> Long.parseLong(
        line.replaceAll(".*_(\\d+)_(\\d+)\\.tar\\.gz", "$2")) == backupId)
      .findFirst();
    if (backup.isEmpty()) {
      return;
    }
    var remotePath = clusterConfiguration.databasePath() + backup.get();
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
    try {
      var process = kubernetesExecutionApi.exec(databasePod,
        new String[] {"ls", clusterConfiguration.databasePath()}, true, true);
      var backups = Lists.<String>newArrayList();
      try (var reader = new BufferedReader(new InputStreamReader(
        process.getInputStream())
      )) {
        backups.addAll(reader.lines().filter(line -> line.contains("snapshot"))
          .toList());
      }
      process.waitFor();
      return backups;
    } catch (Exception exception) {
      errorRepository.processError(exception);
      return Lists.newArrayList();
    }
  }
}
