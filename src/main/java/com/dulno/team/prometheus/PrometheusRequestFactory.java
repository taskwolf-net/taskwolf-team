package com.dulno.team.prometheus;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.net.http.HttpClient;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class PrometheusRequestFactory {
  private final HttpClient httpClient = HttpClient.newHttpClient();

  public PrometheusRequest create(String exporterHost, int exporterPort) {
    return PrometheusRequest.create(httpClient, exporterHost, exporterPort);
  }
}