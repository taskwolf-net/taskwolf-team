package com.dulno.team.prometheus;

import lombok.RequiredArgsConstructor;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(staticName = "create")
public final class PrometheusRequest {
  private final HttpClient httpClient;
  private final String exporterHost;
  private final int exporterPort;

  public CompletableFuture<HttpResponse<String>> send(
    String url, String method
  ) {
    return send(url, method, "");
  }

  public CompletableFuture<HttpResponse<String>> send(
    String url, String method, Map<String, Object> body
  ) {
    return send(url, method, new JSONObject(body).toString());
  }

  private CompletableFuture<HttpResponse<String>> send(
    String url, String method, String body
  ) {
    var requestBuilder = HttpRequest.newBuilder()
      .uri(URI.create("http://" + exporterHost + ":" + exporterPort + url))
      .method(method, HttpRequest.BodyPublishers.ofString(body));
    var httpRequest = requestBuilder.build();
    return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());
  }
}
