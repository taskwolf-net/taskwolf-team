package com.dulno.team.access.maintenance;

import com.dulno.core.maintenance.MaintenanceDatabaseTable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public class MaintenanceContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final MaintenanceDatabaseTable maintenanceDatabaseTable;

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("maintenanceDatabaseTable",
      maintenanceDatabaseTable);
  }
}
