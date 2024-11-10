package com.dulno.team.access.cluster;

import com.dulno.core.iterator.AsyncIterator;
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
    //TODO: Fetch cpu metrics
    return fetchExporterPods()
      .thenCompose(exporters -> fetchMetrics(exporters)
        .thenApply(metrics -> findClusterResources(exporters, metrics)));
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

  private CompletableFuture<List<PrometheusMetrics>> fetchMetrics(
    V1PodList exporters
  ) {
    return AsyncIterator.execute(exporters.getItems(),
      exporter -> prometheusRequestFactory.create(exporter.getStatus().getPodIP(),
          prometheusConfiguration.exporterPort()).send("/metrics", "GET")
        .thenApply(response -> prometheusMetricsFactory.create(
          exporter.getSpec().getNodeName(), response.body())));
  }

  private Map<String, Object> findClusterResources(
    V1PodList exporters, List<PrometheusMetrics> metrics
  ) {
    var resourceInformation = Lists.<Map<String, Object>>newArrayList();
    for (var exporter : exporters.getItems()) {
      var nodeName = exporter.getMetadata().getName();
      var nodeMetrics = metrics.stream()
        .filter(entry -> entry.node().equals(nodeName)).findFirst().get();
      resourceInformation.add(nodeMetrics.assemblyInformation());
    }
    //TODO: Add information like average, max, min, ...
    return Map.of("resources", resourceInformation);
  }
}
