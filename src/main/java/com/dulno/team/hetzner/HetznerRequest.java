package com.dulno.team.hetzner;

import lombok.RequiredArgsConstructor;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(staticName = "create")
public final class HetznerRequest {
  private final HetznerConfiguration hetznerConfiguration;
  private final HttpClient httpClient;

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

  public CompletableFuture<HttpResponse<String>> send(
    String url, String method, String body
  ) {
    return send(url, method, body, "application/json");
  }

  private CompletableFuture<HttpResponse<String>> send(
    String url, String method, String body, String contentType
  ) {
    var requestBuilder = HttpRequest.newBuilder()
      .uri(URI.create("https://api.hetzner.cloud/v1" + url))
      .method(method, HttpRequest.BodyPublishers.ofString(body));
    requestBuilder.setHeader("Authorization",
      "Bearer " + hetznerConfiguration.apiToken());
    requestBuilder.setHeader("Content-Type", contentType);
    var httpRequest = requestBuilder.build();
    return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());
  }
}
