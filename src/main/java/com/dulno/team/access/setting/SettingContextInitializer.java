package com.dulno.team.access.setting;

import com.dulno.team.member.mfa.TeamMultiFactorAuthDatabaseTable;
import com.dulno.team.member.mfa.TeamMultiFactorAuthFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public class SettingContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final TeamMultiFactorAuthDatabaseTable teamMultiFactorAuthDatabaseTable;
  private final TeamMultiFactorAuthFactory teamMultiFactorAuthFactory;

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("teamMultiFactorAuthDatabaseTable",
      teamMultiFactorAuthDatabaseTable);
    beanFactory.registerSingleton("teamMultiFactorAuthFactory",
      teamMultiFactorAuthFactory);
  }
}
