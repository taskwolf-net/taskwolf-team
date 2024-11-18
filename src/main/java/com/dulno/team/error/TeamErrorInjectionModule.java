package com.dulno.team.error;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "create")
public final class TeamErrorInjectionModule extends AbstractModule {
  @Provides
  @Singleton
  TeamErrorConfiguration provideTeamErrorConfiguration() throws Exception {
    return TeamErrorConfiguration.createAndLoad();
  }
}