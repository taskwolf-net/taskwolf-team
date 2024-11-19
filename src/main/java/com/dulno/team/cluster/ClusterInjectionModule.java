package com.dulno.team.cluster;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "create")
public final class ClusterInjectionModule extends AbstractModule {
  @Provides
  @Singleton
  ClusterConfiguration provideClusterConfiguration() throws Exception {
    return ClusterConfiguration.createAndLoad();
  }
}