package com.dulno.team.telegraf;

import lombok.RequiredArgsConstructor;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(staticName = "create")
public final class TelegrafRequest {
  private final HttpClient httpClient;
  private final String telegrafHost;
  private final int telegrafPort;

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
      .uri(URI.create("http://" + telegrafHost + ":" + telegrafPort + url))
      .method(method, HttpRequest.BodyPublishers.ofString(body));
    var httpRequest = requestBuilder.build();
    return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());
  }
}
