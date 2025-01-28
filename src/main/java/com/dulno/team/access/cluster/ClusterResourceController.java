package com.dulno.team.access.cluster;

import com.dulno.core.error.ErrorRepository;
import com.dulno.core.iterator.AsyncIterator;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.dulno.team.telegraf.TelegrafConfiguration;
import com.dulno.team.telegraf.TelegrafMetrics;
import com.dulno.team.telegraf.TelegrafMetricsFactory;
import com.dulno.team.telegraf.TelegrafRequestFactory;
import com.google.common.collect.Maps;
import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1PodList;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.compress.utils.Lists;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class ClusterResourceController extends TeamController {
  private final ErrorRepository errorRepository;
  private final CoreV1Api kubernetesCoreApi;
  private final TelegrafConfiguration telegrafConfiguration;
  private final TelegrafRequestFactory telegrafRequestFactory;
  private final TelegrafMetricsFactory telegrafMetricsFactory;

  private ClusterResourceController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, ErrorRepository errorRepository,
    CoreV1Api kubernetesCoreApi, TelegrafConfiguration telegrafConfiguration,
    TelegrafRequestFactory telegrafRequestFactory,
    TelegrafMetricsFactory telegrafMetricsFactory
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.errorRepository = errorRepository;
    this.kubernetesCoreApi = kubernetesCoreApi;
    this.telegrafConfiguration = telegrafConfiguration;
    this.telegrafRequestFactory = telegrafRequestFactory;
    this.telegrafMetricsFactory = telegrafMetricsFactory;
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
    return fetchTelegrafPods()
      .thenCompose(telegrafPods -> fetchNodes()
        .thenCompose(nodes -> fetchMetrics(telegrafPods)
          .thenApply(metrics -> findClusterResources(telegrafPods, nodes, metrics))));
  }

  private CompletableFuture<V1PodList> fetchTelegrafPods() {
    var futureResponse = new CompletableFuture<V1PodList>();
    try {
      kubernetesCoreApi
        .listNamespacedPod(telegrafConfiguration.kubernetesNamespace())
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

  private CompletableFuture<V1NodeList> fetchNodes() {
    var futureResponse = new CompletableFuture<V1NodeList>();
    try {
      kubernetesCoreApi.listNode().executeAsync(new ApiCallback<>() {
          @Override
          public void onFailure(
            ApiException exception, int statusCode, Map<String,
            List<String>> responseHeaders
          ) {
            errorRepository.processError(exception);
          }

          @Override
          public void onSuccess(
            V1NodeList result, int statusCode,
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

  private CompletableFuture<List<TelegrafMetrics>> fetchMetrics(
    V1PodList telegrafPods
  ) {
    return AsyncIterator.execute(telegrafPods.getItems(),
      telegrafPod -> telegrafRequestFactory.create(telegrafPod.getStatus().getPodIP(),
          telegrafConfiguration.telegrafPort()).send("/metrics", "GET")
        .thenApply(response -> telegrafMetricsFactory.create(
          telegrafPod.getSpec().getNodeName(), response.body())));
  }

  private Map<String, Object> findClusterResources(
    V1PodList telegrafPods, V1NodeList nodes, List<TelegrafMetrics> metrics
  ) {
    var controllerMetrics = Lists.<TelegrafMetrics>newArrayList();
    var workerMetrics = Lists.<TelegrafMetrics>newArrayList();
    for (var telegrafPod : telegrafPods.getItems()) {
      var nodeName = telegrafPod.getSpec().getNodeName();
      var isController = isNodeController(nodes, nodeName);
      var nodeMetrics = metrics.stream()
        .filter(entry -> entry.node().equals(nodeName)).findFirst().get();
      if (isController) {
        controllerMetrics.add(nodeMetrics);
      } else {
        workerMetrics.add(nodeMetrics);
      }
    }
    return Map.of("controller", extractResourceFeatures(controllerMetrics),
      "worker", extractResourceFeatures(workerMetrics));
  }

  private Map<String, Object> extractResourceFeatures(List<TelegrafMetrics> metrics) {
    if (metrics.isEmpty()) {
      return Maps.newHashMap();
    }
    Collections.sort(metrics);
    var result = Maps.<String, Object>newHashMap();
    result.putAll(extractResourceAverage(metrics));
    result.put("maximum", metrics.get(metrics.size() - 1).assemblyInformation());
    result.put("minimum", metrics.get(0).assemblyInformation());
    return result;
  }

  private Map<String, Object> extractResourceAverage(
    List<TelegrafMetrics> metrics
  ) {
    var information = metrics.stream().map(TelegrafMetrics::assemblyInformation)
      .toList();
    var properties = information.get(0).keySet();
    properties.remove("node");
    var average = information.get(0);
    for (var i = 1; i < information.size(); i++) {
      var entry = information.get(i);
      for (var property : properties) {
        average.put(property, (double) average.get(property) +
          (double)entry.get(property));
      }
    }
    for (var property : properties) {
      average.put(property, ((double) average.get(property)) / information.size());
    }
    return Map.of("average", average);
  }

  private boolean isNodeController(V1NodeList nodes, String nodeName) {
    var node = nodes.getItems().stream().filter(entry ->
      entry.getMetadata().getName().equals(nodeName)).findFirst().get();
    return node.getMetadata().getLabels()
      .containsKey("node-role.kubernetes.io/control-plane");
  }
}
