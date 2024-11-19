package com.dulno.team.telegraf;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.net.http.HttpClient;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class TelegrafRequestFactory {
  private final HttpClient httpClient = HttpClient.newHttpClient();

  public TelegrafRequest create(String exporterHost, int exporterPort) {
    return TelegrafRequest.create(httpClient, exporterHost, exporterPort);
  }
}