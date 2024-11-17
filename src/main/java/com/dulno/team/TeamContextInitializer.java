package com.dulno.team;

import com.dulno.core.database.DatabaseConnection;
import com.dulno.core.database.DatabaseKeyspace;
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
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.security.Key;

@Singleton
public class TeamContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final Log log;
  private final Key key;
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

  @Inject
  private TeamContextInitializer(
    Log log, @Named("teamKey") Key key, DatabaseConnection databaseConnection,
    DatabaseKeyspace databaseKeyspace,
    WhitelistConfiguration whitelistConfiguration,
    TeamDatabaseTable teamDatabaseTable, GroupDatabaseTable groupDatabaseTable,
    Translation translation, ErrorRepository errorRepository,
    @Named("verificationMail") Mail verificationMail,
    @Named("supportMail") Mail supportMail, @Named("saleMail") Mail saleMail,
    @Named("changeMail") Mail changeMail
  ) {
    this.log = log;
    this.key = key;
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
  }

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("log", log);
    beanFactory.registerSingleton("key", key);
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
  }
}
