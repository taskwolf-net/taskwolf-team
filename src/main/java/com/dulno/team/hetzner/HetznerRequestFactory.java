package com.dulno.team.hetzner;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.net.http.HttpClient;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class HetznerRequestFactory {
  private final HetznerConfiguration hetznerConfiguration;
  private final HttpClient httpClient = HttpClient.newHttpClient();

  public HetznerRequest create() {
    return HetznerRequest.create(hetznerConfiguration, httpClient);
  }
}
