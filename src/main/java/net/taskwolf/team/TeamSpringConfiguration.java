package net.taskwolf.team;

import jakarta.annotation.PostConstruct;
import net.taskwolf.core.database.DatabaseConnection;
import net.taskwolf.core.database.DatabaseKeyspace;
import net.taskwolf.core.mail.TaskwolfMail;
import net.taskwolf.core.mail.TaskwolfMailConfiguration;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.mail.SupportMailDatabaseTable;
import net.taskwolf.team.mail.SupportMailMessageDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TeamSpringConfiguration {
  @Autowired
  DatabaseConnection databaseConnection;
  @Autowired
  DatabaseKeyspace databaseKeyspace;
  private TeamDatabaseTable teamDatabaseTable;
  private GroupDatabaseTable groupDatabaseTable;
  private TaskwolfMail supportMail;
  private SupportMailDatabaseTable supportMailDatabaseTable;
  private SupportMailMessageDatabaseTable supportMailMessageDatabaseTable;

  @Bean
  TeamDatabaseTable provideTeamDatabaseTable() {
    return teamDatabaseTable;
  }

  @Bean
  GroupDatabaseTable provideGroupDatabaseTable() {
    return groupDatabaseTable;
  }

  @Bean
  @Qualifier("supportMail")
  TaskwolfMail provideSupportMail() {
    return supportMail;
  }

  @Bean
  SupportMailDatabaseTable provideSupportMailDatabaseTable() {
    return supportMailDatabaseTable;
  }

  @Bean
  SupportMailMessageDatabaseTable provideSupportMailMessageDatabaseTable() {
    return supportMailMessageDatabaseTable;
  }

  @PostConstruct
  private void initializeTeamDatabaseTable() {
    teamDatabaseTable = TeamDatabaseTable.create(databaseConnection,
      databaseKeyspace);
    teamDatabaseTable.createIfNotExists();
  }

  @PostConstruct
  private void initializeGroupDatabaseTable() {
    groupDatabaseTable = GroupDatabaseTable.create(databaseConnection,
      databaseKeyspace);
    groupDatabaseTable.createIfNotExists();
  }

  @PostConstruct
  private void initializeSupportMailDatabaseTable() {
    supportMailDatabaseTable = SupportMailDatabaseTable.create(databaseConnection,
      databaseKeyspace);
    supportMailDatabaseTable.createIfNotExists();
  }

  @PostConstruct
  private void initializeSupportMailMessageDatabaseTable() {
    supportMailMessageDatabaseTable = SupportMailMessageDatabaseTable.create(
      databaseConnection, databaseKeyspace);
    supportMailMessageDatabaseTable.createIfNotExists();
  }

  @PostConstruct
  private void initializeSupportMail() throws Exception {
    var mailConfiguration = TaskwolfMailConfiguration.createAndLoad("support");
    supportMail = TaskwolfMail.create(mailConfiguration.mail(),
      mailConfiguration.smtpMailHost(), mailConfiguration.pop3MailHost(),
      mailConfiguration.mailUser(), mailConfiguration.mailPassword());
  }
}
