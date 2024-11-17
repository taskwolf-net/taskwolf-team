package com.dulno.team.access.cluster;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.iterator.AsyncIterator;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.dulno.core.error.ErrorRepository;
import com.google.common.base.CharMatcher;
import com.google.common.collect.Maps;
import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
public final class ClusterUnitController extends TeamController {
  private final ErrorRepository errorRepository;
  private final CoreV1Api kubernetesCoreApi;
  private final AppsV1Api kubernetesAppsApi;
  private final BatchV1Api kubernetesBatchApi;

  private ClusterUnitController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, ErrorRepository errorRepository,
    CoreV1Api kubernetesCoreApi, AppsV1Api kubernetesAppsApi,
    BatchV1Api kubernetesBatchApi
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.errorRepository = errorRepository;
    this.kubernetesCoreApi = kubernetesCoreApi;
    this.kubernetesAppsApi = kubernetesAppsApi;
    this.kubernetesBatchApi = kubernetesBatchApi;
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
            errorRepository.processError(exception);
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
      errorRepository.processError(exception);
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
      case "REPLICASET" -> deploymentInformation(unitName.substring(0,
        unitName.lastIndexOf('-')));
      case "DEPLOYMENT" -> deploymentInformation(unitName);
      case "STATEFULSET" -> statefulSetInformation(unitName);
      case "DAEMONSET" -> daemonSetInformation(unitName);
      case "JOB" -> jobInformation(unitName);
      default -> CompletableFuture.completedFuture(Maps.newHashMap());
    };
  }

  private CompletableFuture<Map<String, Object>> deploymentInformation(
    String unitName
  ) {
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    try {
      kubernetesAppsApi.readNamespacedDeployment(unitName, CLUSTER_POD_NAMESPACE)
        .executeAsync(new ApiCallback<>() {
          @Override
          public void onFailure(
            ApiException exception, int statusCode, Map<String,
            List<String>> responseHeaders
          ) {
            errorRepository.processError(exception);
          }

          @Override
          public void onSuccess(
            V1Deployment result, int statusCode,
            Map<String, List<String>> responseHeaders
          ) {
            futureResponse.complete(Map.of("name", unitName, "type", "DEPLOYMENT",
              "desiredReplicas", result.getSpec().getReplicas() != null ?
                result.getSpec().getReplicas() : 0,
              "availableReplicas",result.getStatus().getReadyReplicas() != null ?
                result.getStatus().getReadyReplicas() : 0));
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
      errorRepository.processError(exception);
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
            errorRepository.processError(exception);
          }

          @Override
          public void onSuccess(
            V1StatefulSet result, int statusCode,
            Map<String, List<String>> responseHeaders
          ) {
            futureResponse.complete(Map.of("name", unitName, "type", "STATEFULSET",
              "desiredReplicas", result.getSpec().getReplicas() != null ?
                result.getSpec().getReplicas() : 0,
              "availableReplicas",result.getStatus().getReadyReplicas() != null ?
                result.getStatus().getReadyReplicas() : 0));
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
      errorRepository.processError(exception);
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
            errorRepository.processError(exception);
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
      errorRepository.processError(exception);
      futureResponse.complete(Maps.newHashMap());
    }
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> jobInformation(
    String unitName
  ) {
    if (CharMatcher.is('-').countIn(unitName) == 3) {
      unitName = unitName.substring(0, unitName.lastIndexOf('-'));
    }
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
            errorRepository.processError(exception);
          }

          @Override
          public void onSuccess(
            V1PodList result, int statusCode,
            Map<String, List<String>> responseHeaders
          ) {
            futureResponse.complete(result.getItems().stream()
              .filter(pod -> pod.getMetadata().getOwnerReferences().stream()
                .anyMatch(owner -> owner.getName().contains(unitName) &&
                  checkUnitOwnerMatch(owner, unitType)))
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
      errorRepository.processError(exception);
      futureResponse.complete(Lists.newArrayList());
    }
    return futureResponse;
  }

  private boolean checkUnitOwnerMatch(V1OwnerReference owner, String unitType) {
    if (unitType.equalsIgnoreCase(owner.getKind())) {
      return true;
    }
    if (unitType.equalsIgnoreCase("DEPLOYMENT") &&
      owner.getKind().equalsIgnoreCase("REPLICASET")
    ) {
      return true;
    }
    return unitType.equalsIgnoreCase("REPLICASET") &&
      owner.getKind().equalsIgnoreCase("DEPLOYMENT");
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

  @RequestMapping(path = "/cluster/unit/restart/", method = RequestMethod.POST)
  public void restartClusterUnit(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    memberHasPermission(findMemberId(request), "cluster.unit.restart")
      .thenAcceptAsync(hasPermission -> restartClusterUnit(
        body.getString("unitName"), body.getString("unitType"),
        hasPermission));
  }

  private void restartClusterUnit(
    String unitName, String unitType, boolean hasPermission
  ) {
    if (!hasPermission) {
      return;
    }
    try {
      switch (unitType.toUpperCase()) {
        case "DEPLOYMENT" -> restartDeployment(unitName);
        case "STATEFULSET" -> restartStatefulSet(unitName);
        case "DAEMONSET" -> restartDaemonSet(unitName);
        case "JOB" -> restartJob(unitName);
      }
    } catch (Exception exception) {
      errorRepository.processError(exception);
    }
  }

  private void restartDeployment(String unitName) throws Exception {
    var deployment = kubernetesAppsApi.readNamespacedDeployment(unitName,
      CLUSTER_POD_NAMESPACE).execute();
    var annotations = new HashMap<>(deployment.getSpec().getTemplate()
      .getMetadata().getAnnotations());
    annotations.put("kubectl.kubernetes.io/restartedAt",
      String.valueOf(System.currentTimeMillis()));
    deployment.getSpec().getTemplate().getMetadata().setAnnotations(annotations);
    kubernetesAppsApi.replaceNamespacedDeployment(unitName,
      CLUSTER_POD_NAMESPACE, deployment).execute();
  }

  private void restartStatefulSet(String unitName) throws Exception {
    var statefulSet = kubernetesAppsApi.readNamespacedStatefulSet(unitName,
      CLUSTER_POD_NAMESPACE).execute();
    var annotations = new HashMap<>(statefulSet.getSpec().getTemplate()
      .getMetadata().getAnnotations());
    annotations.put("kubectl.kubernetes.io/restartedAt",
      String.valueOf(System.currentTimeMillis()));
    statefulSet.getSpec().getTemplate().getMetadata().setAnnotations(annotations);
    kubernetesAppsApi.replaceNamespacedStatefulSet(unitName,
      CLUSTER_POD_NAMESPACE, statefulSet).execute();
  }

  private void restartDaemonSet(String unitName) throws Exception {
    var daemonSet = kubernetesAppsApi.readNamespacedDaemonSet(unitName,
      CLUSTER_POD_NAMESPACE).execute();
    var annotations = new HashMap<>(daemonSet.getSpec().getTemplate()
      .getMetadata().getAnnotations());
    annotations.put("kubectl.kubernetes.io/restartedAt",
      String.valueOf(System.currentTimeMillis()));
    daemonSet.getSpec().getTemplate().getMetadata().setAnnotations(annotations);
    kubernetesAppsApi.replaceNamespacedDaemonSet(unitName,
      CLUSTER_POD_NAMESPACE, daemonSet).execute();
  }

  private void restartJob(String unitName) throws Exception {
    deleteCurrentJobs(unitName);
    var cronJob = kubernetesBatchApi.readNamespacedCronJob(unitName,
      CLUSTER_POD_NAMESPACE).execute();
    var job = new V1Job()
      .apiVersion("batch/v1")
      .kind("Job")
      .metadata(new V1ObjectMeta()
        .name(unitName + "-" + UUID.randomUUID().toString().substring(0, 8))
        .namespace(CLUSTER_POD_NAMESPACE)
        .putLabelsItem("cronjob-name", unitName))
      .spec(cronJob.getSpec().getJobTemplate().getSpec());
    var ownerReference = new V1OwnerReference()
      .apiVersion("batch/v1")
      .kind("CronJob")
      .name(cronJob.getMetadata().getName())
      .uid(cronJob.getMetadata().getUid())
      .controller(false)
      .blockOwnerDeletion(false);
    job.getMetadata().setOwnerReferences(Collections.singletonList(ownerReference));
    kubernetesBatchApi.createNamespacedJob(CLUSTER_POD_NAMESPACE, job).execute();
  }

  private void deleteCurrentJobs(String unitName) throws Exception {
    var jobList = kubernetesBatchApi.listNamespacedJob(CLUSTER_POD_NAMESPACE)
      .execute();
    for (var job : jobList.getItems()) {
      if (job.getMetadata().getName().contains(unitName)) {
        var jobName = job.getMetadata().getName();
        var podList = kubernetesCoreApi.listNamespacedPod(CLUSTER_POD_NAMESPACE)
          .execute();
        for (var pod : podList.getItems()) {
          var labels = pod.getMetadata().getLabels();
          if (labels.containsKey("job-name") && labels.get("job-name").equals(jobName)) {
            kubernetesCoreApi.deleteNamespacedPod(pod.getMetadata().getName(),
              CLUSTER_POD_NAMESPACE).execute();
          }
        }
        kubernetesBatchApi.deleteNamespacedJob(jobName, CLUSTER_POD_NAMESPACE)
          .execute();
      }
    }
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
            errorRepository.processError(exception);
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
      errorRepository.processError(exception);
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
            errorRepository.processError(exception);
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
      errorRepository.processError(exception);
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
      .thenAcceptAsync(hasPermission -> deleteClusterPod(body.getString("podName"),
        hasPermission));
  }

  private void deleteClusterPod(
    String podName, boolean hasPermission
  ) {
    if (!hasPermission) {
      return;
    }
    try {
      kubernetesCoreApi.deleteNamespacedPod(podName, CLUSTER_POD_NAMESPACE)
        .execute();
    } catch (Exception exception) {
      errorRepository.processError(exception);
    }
  }
}
