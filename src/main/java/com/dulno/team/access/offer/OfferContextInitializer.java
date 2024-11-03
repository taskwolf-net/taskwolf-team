package com.dulno.team.access.offer;

import com.dulno.core.offer.OfferDatabaseTable;
import com.dulno.team.offer.TeamOfferConfiguration;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public class OfferContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final OfferDatabaseTable offerDatabaseTable;
  private final TeamOfferConfiguration offerConfiguration;

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("offerDatabaseTable", offerDatabaseTable);
    beanFactory.registerSingleton("offerConfiguration", offerConfiguration);
  }
}
