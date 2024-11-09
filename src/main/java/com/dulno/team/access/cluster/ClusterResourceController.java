package com.dulno.team.access.cluster;

import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.dulno.team.prometheus.PrometheusConfiguration;
import com.dulno.team.prometheus.PrometheusMetrics;
import com.dulno.team.prometheus.PrometheusMetricsFactory;
import com.dulno.team.prometheus.PrometheusRequestFactory;
import com.google.common.collect.Maps;
import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1PodList;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.compress.utils.Lists;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class ClusterResourceController extends TeamController {
  private final CoreV1Api kubernetesCoreApi;
  private final PrometheusConfiguration prometheusConfiguration;
  private final PrometheusRequestFactory prometheusRequestFactory;
  private final PrometheusMetricsFactory prometheusMetricsFactory;

  private ClusterResourceController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, CoreV1Api kubernetesCoreApi,
    PrometheusConfiguration prometheusConfiguration,
    PrometheusRequestFactory prometheusRequestFactory,
    PrometheusMetricsFactory prometheusMetricsFactory
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.kubernetesCoreApi = kubernetesCoreApi;
    this.prometheusConfiguration = prometheusConfiguration;
    this.prometheusRequestFactory = prometheusRequestFactory;
    this.prometheusMetricsFactory = prometheusMetricsFactory;
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
    //TODO
    return null;
    /*return fetchExporterPods()
      .thenCompose(pods -> fetchMetrics()
        .thenApply(metrics -> findClusterResources(nodes, metrics)));*/
  }

  private CompletableFuture<V1PodList> fetchExporterPods() {
    var futureResponse = new CompletableFuture<V1PodList>();
    try {
      kubernetesCoreApi
        .listNamespacedPod(prometheusConfiguration.kubernetesNamespace())
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

  private Map<String, Object> findClusterResources(
    V1PodList exporters, List<PrometheusMetrics> metrics
  ) {
    var resourceInformation = Lists.<Map<String, Object>>newArrayList();
    //TODO
    /*for (var node : nodes.getItems()) {
      var nodeName = node.getMetadata().getName();
      var status = node.getStatus();
      var cpuCapacity = status.getCapacity().get("cpu").getNumber().doubleValue();
      var memoryCapacity = status.getCapacity().get("memory").getNumber()
        .doubleValue();
      var nodeMetrics = metricsList.getItems().stream()
        .filter(metrics -> metrics.getMetadata().getName().equals(nodeName))
        .findFirst().get();
      var usage = nodeMetrics.getUsage();
      var cpuUsage = usage.get("cpu").getNumber().doubleValue();
      var memoryUsage = usage.get("memory").getNumber().doubleValue();
      resourceInformation.add(assemblyNodeResourceInformation(nodeName,
        cpuCapacity, cpuUsage, memoryCapacity, memoryUsage));
    }*/
    return Map.of("resources", resourceInformation);
  }

  private Map<String, Object> assemblyNodeResourceInformation(
    String nodeName, double cpuCapacity, double cpuUsage, double memoryCapacity,
    double memoryUsage
  ) {
    var nodeInformation = Maps.<String, Object>newHashMap();
    nodeInformation.put("nodeName", nodeName);
    nodeInformation.put("cpuCapacity", cpuCapacity);
    nodeInformation.put("cpuUsage", cpuUsage);
    nodeInformation.put("cpuRatio", cpuUsage / cpuCapacity);
    nodeInformation.put("memoryCapacity", memoryCapacity);
    nodeInformation.put("memoryUsage", memoryUsage);
    nodeInformation.put("memoryRatio", memoryUsage / memoryCapacity);
    return nodeInformation;
  }
}
