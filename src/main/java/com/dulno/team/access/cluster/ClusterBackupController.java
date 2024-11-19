package com.dulno.team.access.cluster;

import com.dulno.core.error.ErrorRepository;
import com.dulno.team.access.TeamController;
import com.dulno.team.cluster.ClusterConfiguration;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class ClusterBackupController extends TeamController {
  private final ErrorRepository errorRepository;
  private final CoreV1Api kubernetesCoreApi;
  private final Exec kubernetesExecutionApi;
  private final ClusterConfiguration clusterConfiguration;

  private ClusterBackupController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, ErrorRepository errorRepository,
    CoreV1Api kubernetesCoreApi, Exec kubernetesExecutionApi,
    ClusterConfiguration clusterConfiguration
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.errorRepository = errorRepository;
    this.kubernetesCoreApi = kubernetesCoreApi;
    this.kubernetesExecutionApi = kubernetesExecutionApi;
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
      for (var backup : fetchDatabaseBackups(pod)) {
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

  private List<Long> fetchDatabaseBackups(V1Pod databasePod) {
    try {
      var process = kubernetesExecutionApi.exec(databasePod,
        new String[] {"ls", "/var/lib/cassandra/data"}, true, true);
      var backups = Lists.<Long>newArrayList();
      try (var reader = new BufferedReader(new InputStreamReader(
        process.getInputStream())
      )) {
        backups.addAll(reader.lines().filter(line -> line.contains("snapshot"))
          .map(line -> line.replaceAll(".*_(\\d+)_(\\d+)\\.tar\\.gz", "$2"))
          .map(Long::parseLong).toList());
      }
      process.waitFor();
      return backups;
    } catch (Exception exception) {
      errorRepository.processError(exception);
      return Lists.newArrayList();
    }
  }
}
