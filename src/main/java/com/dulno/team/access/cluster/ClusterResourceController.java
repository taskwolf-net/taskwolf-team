package com.dulno.team.access.cluster;

import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.google.common.collect.Maps;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1NodeStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class ClusterResourceController extends TeamController {
  private final CoreV1Api kubernetesApi;

  private ClusterResourceController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, CoreV1Api kubernetesApi
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.kubernetesApi = kubernetesApi;
  }

  @RequestMapping(path = "/cluster/resources/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findClusterResources(
    HttpServletRequest request
  ) {
    return memberHasPermission(findMemberId(request), "cluster.resources")
      .thenCompose(this::findClusterResources);
  }

  private CompletableFuture<Map<String, Object>> findClusterResources(
    boolean hasPermission
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    try {
      kubernetesApi.listNode().executeAsync(new ApiCallback<>() {
        @Override
        public void onFailure(
          ApiException exception, int statusCode, Map<String,
          List<String>> responseHeaders
        ) {
          exception.printStackTrace();
        }

        @Override
        public void onSuccess(
          V1NodeList result, int statusCode,
          Map<String, List<String>> responseHeaders
        ) {
          futureResponse.complete(findClusterResources(result));
        }

        @Override
        public void onUploadProgress(
          long bytesWritten, long contentLength, boolean done
        ) {
        }

        @Override
        public void onDownloadProgress(
          long bytesRead, long contentLength, boolean done
        ) {
        }
      });
    } catch (Exception exception) {
      exception.printStackTrace();
    }
    return futureResponse;
  }

  private Map<String, Object> findClusterResources(V1NodeList nodes) {
    for (var node : nodes.getItems()) {
      String nodeName = node.getMetadata().getName();
      V1NodeStatus status = node.getStatus();

      // Get CPU capacity and usage
      Quantity cpuCapacity = status.getCapacity().get("cpu");
      Quantity cpuAllocatable = status.getAllocatable().get("cpu");

      // Get Memory capacity and usage
      Quantity memoryCapacity = status.getCapacity().get("memory");
      Quantity memoryAllocatable = status.getAllocatable().get("memory");

      // Disk storage (if available)
      Quantity ephemeralStorageCapacity = status.getCapacity().get("ephemeral-storage");
      Quantity ephemeralStorageAllocatable = status.getAllocatable().get("ephemeral-storage");

      System.out.println("Node: " + nodeName);
      System.out.println("CPU Capacity: " + cpuCapacity.getNumber());
      System.out.println("CPU Allocatable: " + cpuAllocatable.getNumber());
      System.out.println("Memory Capacity: " + memoryCapacity.getNumber() + " bytes");
      System.out.println("Memory Allocatable: " + memoryAllocatable.getNumber() + " bytes");
      if (ephemeralStorageCapacity != null) {
        System.out.println("Ephemeral Storage Capacity: " + ephemeralStorageCapacity.getNumber() + " bytes");
      }
      if (ephemeralStorageAllocatable != null) {
        System.out.println("Ephemeral Storage Allocatable: " + ephemeralStorageAllocatable.getNumber() + " bytes");
      }
      System.out.println("---------------------------");
    }
    return Maps.newHashMap();
  }
}
