package com.dulno.team.hetzner;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "create")
public final class HetznerInjectionModule extends AbstractModule {
  @Provides
  @Singleton
  HetznerConfiguration provideHetznerConfiguration() throws Exception {
    return HetznerConfiguration.createAndLoad();
  }
}