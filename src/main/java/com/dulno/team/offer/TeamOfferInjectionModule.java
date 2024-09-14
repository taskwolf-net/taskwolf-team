package com.dulno.team.offer;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "create")
public final class TeamOfferInjectionModule extends AbstractModule {
  @Provides
  @Singleton
  TeamOfferConfiguration provideTeamOfferConfiguration() throws Exception {
    return TeamOfferConfiguration.createAndLoad();
  }
}