package com.dulno.team.telegraf;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "create")
public final class TelegrafInjectionModule extends AbstractModule {
  @Provides
  @Singleton
  TelegrafConfiguration provideTelegrafConfiguration() throws Exception {
    return TelegrafConfiguration.createAndLoad();
  }
}