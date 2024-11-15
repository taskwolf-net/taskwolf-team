package com.dulno.team.access.cluster;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.iterator.AsyncIterator;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.google.common.collect.Maps;
import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.compress.utils.Lists;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class ClusterUnitController extends TeamController {
  private final CoreV1Api kubernetesCoreApi;
  private final AppsV1Api kubernetesAppsApi;

  private ClusterUnitController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, CoreV1Api kubernetesCoreApi,
    AppsV1Api kubernetesAppsApi
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.kubernetesCoreApi = kubernetesCoreApi;
    this.kubernetesAppsApi = kubernetesAppsApi;
  }

  @RequestMapping(path = "/cluster/units/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findClusterUnits(
    HttpServletRequest request
  ) {
    return memberHasPermission(findMemberId(request), "cluster.units")
      .thenCompose(this::findClusterUnits);
  }

  private static final String CLUSTER_POD_NAMESPACE = "default";

  private CompletableFuture<Map<String, Object>> findClusterUnits(
    boolean hasPermission
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    try {
      kubernetesCoreApi.listNamespacedPod(CLUSTER_POD_NAMESPACE)
        .executeAsync(new ApiCallback<>() {
          @Override
          public void onFailure(
            ApiException exception, int statusCode, Map<String,
            List<String>> responseHeaders
          ) {
            exception.printStackTrace();
          }

          @Override
          public void onSuccess(
            V1PodList result, int statusCode,
            Map<String, List<String>> responseHeaders
          ) {
            findClusterUnits(result).thenAccept(futureResponse::complete);
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
      futureResponse.complete(Maps.newHashMap());
    }
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> findClusterUnits(V1PodList pods) {
    var units = Maps.<String, String>newHashMap();
    for (var pod : pods.getItems()) {
      if (pod.getMetadata() == null) {
        continue;
      }
      var owners = pod.getMetadata().getOwnerReferences();
      if (owners == null) {
        continue;
      }
      for (var owner : owners) {
        units.put(owner.getName(), owner.getKind());
      }
    }
    return AsyncIterator.execute(new ArrayList<>(units.entrySet()),
        unit -> superficialUnitInformation(unit.getKey(), unit.getValue()))
      .thenApply(result -> Map.of("units", result));
  }

  private CompletableFuture<Map<String, Object>> superficialUnitInformation(
    String unitName, String unitType
  ) {
    return switch (unitType.toUpperCase()) {
      case "REPLICASET" -> replicaSetInformation(unitName);
      case "STATEFULSET" -> statefulSetInformation(unitName);
      case "DAEMONSET" -> daemonSetInformation(unitName);
      case "JOB" -> jobInformation(unitName);
      default -> CompletableFuture.completedFuture(Maps.newHashMap());
    };
  }

  private CompletableFuture<Map<String, Object>> replicaSetInformation(
    String unitName
  ) {
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    try {
      kubernetesAppsApi.readNamespacedReplicaSet(unitName, CLUSTER_POD_NAMESPACE)
        .executeAsync(new ApiCallback<>() {
          @Override
          public void onFailure(
            ApiException exception, int statusCode, Map<String,
            List<String>> responseHeaders
          ) {
            exception.printStackTrace();
          }

          @Override
          public void onSuccess(
            V1ReplicaSet result, int statusCode,
            Map<String, List<String>> responseHeaders
          ) {
            futureResponse.complete(Map.of("name", unitName, "type", "REPLICASET",
              "desiredReplicas", result.getSpec().getReplicas(),
              "availableReplicas", result.getStatus().getReadyReplicas()));
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
      futureResponse.complete(Maps.newHashMap());
    }
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> statefulSetInformation(
    String unitName
  ) {
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    try {
      kubernetesAppsApi.readNamespacedStatefulSet(unitName, CLUSTER_POD_NAMESPACE)
        .executeAsync(new ApiCallback<>() {
          @Override
          public void onFailure(
            ApiException exception, int statusCode, Map<String,
            List<String>> responseHeaders
          ) {
            exception.printStackTrace();
          }

          @Override
          public void onSuccess(
            V1StatefulSet result, int statusCode,
            Map<String, List<String>> responseHeaders
          ) {
            futureResponse.complete(Map.of("name", unitName, "type", "STATEFULSET",
              "desiredReplicas", result.getSpec().getReplicas(),
              "availableReplicas", result.getStatus().getReadyReplicas()));
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
      futureResponse.complete(Maps.newHashMap());
    }
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> daemonSetInformation(
    String unitName
  ) {
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    try {
      kubernetesAppsApi.readNamespacedDaemonSet(unitName, CLUSTER_POD_NAMESPACE)
        .executeAsync(new ApiCallback<>() {
          @Override
          public void onFailure(
            ApiException exception, int statusCode, Map<String,
            List<String>> responseHeaders
          ) {
            exception.printStackTrace();
          }

          @Override
          public void onSuccess(
            V1DaemonSet result, int statusCode,
            Map<String, List<String>> responseHeaders
          ) {
            futureResponse.complete(Map.of("name", unitName, "type", "DAEMONSET",
              "desiredReplicas", result.getStatus().getDesiredNumberScheduled(),
              "availableReplicas", result.getStatus().getNumberReady()));
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
      futureResponse.complete(Maps.newHashMap());
    }
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> jobInformation(
    String unitName
  ) {
    return CompletableFuture.completedFuture(Map.of("name", unitName,
      "type", "JOB"));
  }

  @RequestMapping(path = "/cluster/unit/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findClusterUnit(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    return memberHasPermission(findMemberId(request), "cluster.unit")
      .thenCompose(hasPermission ->
        findClusterUnit(body.getString("unitName"),
          body.getString("unitType"), hasPermission));
  }

  private CompletableFuture<Map<String, Object>> findClusterUnit(
    String unitName, String unitType, boolean hasPermission
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return superficialUnitInformation(unitName, unitType)
      .thenCompose(superficialInformation -> fetchClusterUnitPods(unitName, unitType)
        .thenApply(pods -> detailedUnitInformation(superficialInformation, pods)));
  }

  private CompletableFuture<List<V1Pod>> fetchClusterUnitPods(
    String unitName, String unitType
  ) {
    var futureResponse = new CompletableFuture<List<V1Pod>>();
    try {
      kubernetesCoreApi.listNamespacedPod(CLUSTER_POD_NAMESPACE)
        .executeAsync(new ApiCallback<>() {
          @Override
          public void onFailure(
            ApiException exception, int statusCode, Map<String,
            List<String>> responseHeaders
          ) {
            exception.printStackTrace();
          }

          @Override
          public void onSuccess(
            V1PodList result, int statusCode,
            Map<String, List<String>> responseHeaders
          ) {
            futureResponse.complete(result.getItems().stream()
              .filter(pod -> pod.getMetadata().getOwnerReferences().stream()
                .anyMatch(owner -> owner.getName().equalsIgnoreCase(unitName) &&
                  owner.getKind().equalsIgnoreCase(unitType)))
              .toList());
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
      futureResponse.complete(Lists.newArrayList());
    }
    return futureResponse;
  }

  private Map<String, Object> detailedUnitInformation(
    Map<String, Object> superficialInformation, List<V1Pod> unitPods
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.putAll(superficialInformation);
    information.put("pods", unitPods.stream().map(this::superficialPodInformation)
      .toList());
    return information;
  }

  private Map<String, Object> superficialPodInformation(V1Pod pod) {
    var information = Maps.<String, Object>newHashMap();
    information.put("name", pod.getMetadata().getName());
    information.put("node", pod.getSpec().getNodeName());
    information.put("phase", pod.getStatus().getPhase());
    information.put("startTime", pod.getStatus().getStartTime().toEpochSecond()
      * 1000);
    information.put("ipAddress", pod.getStatus().getPodIP());
    return information;
  }

  @RequestMapping(path = "/cluster/pod/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findClusterPod(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    return memberHasPermission(findMemberId(request), "cluster.pod")
      .thenCompose(hasPermission -> findClusterPod(body.getString("podName"),
        hasPermission));
  }

  private CompletableFuture<Map<String, Object>> findClusterPod(
    String podName, boolean hasPermission
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return fetchClusterPod(podName)
      .thenCompose(pod -> fetchClusterPodLog(podName)
        .thenApply(log -> detailedPodInformation(pod, log)));
  }

  private CompletableFuture<V1Pod> fetchClusterPod(String podName) {
    var futureResponse = new CompletableFuture<V1Pod>();
    try {
      kubernetesCoreApi.readNamespacedPod(podName, CLUSTER_POD_NAMESPACE)
        .executeAsync(new ApiCallback<>() {
          @Override
          public void onFailure(
            ApiException exception, int statusCode, Map<String,
            List<String>> responseHeaders
          ) {
            exception.printStackTrace();
          }

          @Override
          public void onSuccess(
            V1Pod result, int statusCode,
            Map<String, List<String>> responseHeaders
          ) {
            futureResponse.complete(result);
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
      futureResponse.complete(null);
    }
    return futureResponse;
  }

  private CompletableFuture<String> fetchClusterPodLog(String podName) {
    var futureResponse = new CompletableFuture<String>();
    try {
      kubernetesCoreApi.readNamespacedPodLog(podName, CLUSTER_POD_NAMESPACE)
        .executeAsync(new ApiCallback<>() {
          @Override
          public void onFailure(
            ApiException exception, int statusCode, Map<String,
            List<String>> responseHeaders
          ) {
            exception.printStackTrace();
          }

          @Override
          public void onSuccess(
            String result, int statusCode,
            Map<String, List<String>> responseHeaders
          ) {
            futureResponse.complete(result);
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
      futureResponse.complete("");
    }
    return futureResponse;
  }

  private Map<String, Object> detailedPodInformation(V1Pod pod, String log) {
    var information = Maps.<String, Object>newHashMap();
    information.putAll(superficialPodInformation(pod));
    information.put("containers", pod.getSpec().getContainers().stream()
      .map(this::containerInformation).toList());
    information.put("log", log);
    return information;
  }

  private Map<String, Object> containerInformation(V1Container container) {
    var information = Maps.<String, Object>newHashMap();
    information.put("name", container.getName());
    information.put("image", container.getImage());
    return information;
  }

  @RequestMapping(path = "/cluster/pod/delete/", method = RequestMethod.POST)
  public void deleteClusterPod(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    memberHasPermission(findMemberId(request), "cluster.pod.delete")
      .thenAccept(hasPermission -> deleteClusterPod(body.getString("podName"),
        hasPermission));
  }

  private void deleteClusterPod(
    String podName, boolean hasPermission
  ) {
    if (!hasPermission) {
      return;
    }
    deleteClusterPod(podName);
  }

  private void deleteClusterPod(String podName) {
    try {
      kubernetesCoreApi.deleteNamespacedPod(podName, CLUSTER_POD_NAMESPACE)
        .executeAsync(new ApiCallback<>() {
          @Override
          public void onFailure(
            ApiException exception, int statusCode, Map<String,
            List<String>> responseHeaders
          ) {
            exception.printStackTrace();
          }

          @Override
          public void onSuccess(
            V1Pod result, int statusCode,
            Map<String, List<String>> responseHeaders
          ) {
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
  }
}
