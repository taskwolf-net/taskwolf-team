package com.dulno.team.prometheus;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "create")
public final class PrometheusInjectionModule extends AbstractModule {
  @Provides
  @Singleton
  PrometheusConfiguration providePrometheusConfiguration() throws Exception {
    return PrometheusConfiguration.createAndLoad();
  }
}