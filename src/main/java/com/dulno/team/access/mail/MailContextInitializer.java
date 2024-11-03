package com.dulno.team.access.mail;

import com.dulno.core.mail.MailFactory;
import com.dulno.core.mail.OutgoingMailDatabaseTable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public class MailContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final OutgoingMailDatabaseTable outgoingMailDatabaseTable;
  private final MailFactory mailFactory;

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("outgoingMailDatabaseTable",
      outgoingMailDatabaseTable);
    beanFactory.registerSingleton("mailFactory", mailFactory);
  }
}
