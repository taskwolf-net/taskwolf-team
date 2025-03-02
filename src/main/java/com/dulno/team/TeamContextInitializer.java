package com.dulno.team;

import com.dulno.core.database.DatabaseConnection;
import com.dulno.core.database.DatabaseKeyspace;
import com.dulno.core.environment.DulnoEnvironment;
import com.dulno.core.hashing.Hashing;
import com.dulno.core.locale.Translation;
import com.dulno.core.error.ErrorRepository;
import com.dulno.core.log.Log;
import com.dulno.core.mail.Mail;
import com.dulno.core.whitelist.WhitelistConfiguration;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.security.Key;

@Singleton
public class TeamContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final Log log;
  private final Key panelKey;
  private final Key refreshKey;
  private final DatabaseConnection databaseConnection;
  private final DatabaseKeyspace databaseKeyspace;
  private final WhitelistConfiguration whitelistConfiguration;
  private final TeamDatabaseTable teamDatabaseTable;
  private final GroupDatabaseTable groupDatabaseTable;
  private final Translation translation;
  private final ErrorRepository errorRepository;
  private final Mail verificationMail;
  private final Mail supportMail;
  private final Mail saleMail;
  private final Mail changeMail;
  private final Hashing hashing;
  private final DulnoEnvironment environment;

  @Inject
  private TeamContextInitializer(
    Log log, @Named("panelKey") Key panelKey, @Named("panelRefreshKey") Key refreshKey,
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace,
    WhitelistConfiguration whitelistConfiguration,
    TeamDatabaseTable teamDatabaseTable, GroupDatabaseTable groupDatabaseTable,
    Translation translation, ErrorRepository errorRepository,
    @Named("verificationMail") Mail verificationMail,
    @Named("supportMail") Mail supportMail, @Named("saleMail") Mail saleMail,
    @Named("changeMail") Mail changeMail, Hashing hashing,
    DulnoEnvironment environment
  ) {
    this.log = log;
    this.panelKey = panelKey;
    this.refreshKey = refreshKey;
    this.databaseConnection = databaseConnection;
    this.databaseKeyspace = databaseKeyspace;
    this.whitelistConfiguration = whitelistConfiguration;
    this.teamDatabaseTable = teamDatabaseTable;
    this.groupDatabaseTable = groupDatabaseTable;
    this.translation = translation;
    this.errorRepository = errorRepository;
    this.verificationMail = verificationMail;
    this.supportMail = supportMail;
    this.saleMail = saleMail;
    this.changeMail = changeMail;
    this.hashing = hashing;
    this.environment = environment;
  }

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("log", log);
    beanFactory.registerSingleton("databaseConnection", databaseConnection);
    beanFactory.registerSingleton("databaseKeyspace", databaseKeyspace);
    beanFactory.registerSingleton("whitelistConfiguration",
      whitelistConfiguration);
    beanFactory.registerSingleton("teamDatabaseTable", teamDatabaseTable);
    beanFactory.registerSingleton("groupDatabaseTable", groupDatabaseTable);
    beanFactory.registerSingleton("translation", translation);
    beanFactory.registerSingleton("errorRepository", errorRepository);
    beanFactory.registerSingleton("verificationMail", verificationMail);
    beanFactory.registerSingleton("supportMail", supportMail);
    beanFactory.registerSingleton("saleMail", saleMail);
    beanFactory.registerSingleton("changeMail", changeMail);
    beanFactory.registerSingleton("hashing", hashing);
    beanFactory.registerSingleton("dulnoEnvironment", environment);
    applicationContext.addBeanFactoryPostProcessor(
      new KeyPostProcessor(panelKey, refreshKey));
  }

  private final class KeyPostProcessor implements BeanDefinitionRegistryPostProcessor {
    private final Key panelKey;
    private final Key refreshKey;

    private KeyPostProcessor(Key panelKey, Key refreshKey) {
      this.panelKey = panelKey;
      this.refreshKey = refreshKey;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(
      BeanDefinitionRegistry registry
    ) throws BeansException {
      registerKey(registry, "panelKey", panelKey, true);
      registerKey(registry, "panelRefreshKey", refreshKey, false);
    }

    private void registerKey(
      BeanDefinitionRegistry registry, String name, Key key, boolean isPrimary
    ) {
      var definition = BeanDefinitionBuilder.rootBeanDefinition(Key.class)
        .getBeanDefinition();
      definition.setInstanceSupplier(() -> key);
      definition.setPrimary(isPrimary);
      registry.registerBeanDefinition(name, definition);
    }
  }
}
