package net.taskwolf.team;

import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import jakarta.annotation.PostConstruct;
import net.taskwolf.core.database.DatabaseConnection;
import net.taskwolf.core.database.DatabaseKeyspace;
import net.taskwolf.core.mail.TaskwolfMail;
import net.taskwolf.core.mail.TaskwolfMailConfiguration;
import net.taskwolf.core.user.ProfilePictureDatabaseTable;
import net.taskwolf.core.user.User;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.mail.SupportMailDatabaseTable;
import net.taskwolf.team.mail.SupportMailMessageDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import net.taskwolf.team.member.TeamWorkDatabaseTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Configuration
public class TeamSpringConfiguration {
  @Autowired
  DatabaseConnection databaseConnection;
  @Autowired
  DatabaseKeyspace databaseKeyspace;
  @Autowired
  UserDatabaseTable userDatabaseTable;
  @Autowired
  ProfilePictureDatabaseTable profilePictureDatabaseTable;
  private TeamDatabaseTable teamDatabaseTable;
  private GroupDatabaseTable groupDatabaseTable;
  private TaskwolfMail supportMail;
  private SupportMailDatabaseTable supportMailDatabaseTable;
  private SupportMailMessageDatabaseTable supportMailMessageDatabaseTable;
  private TeamWorkDatabaseTable teamWorkDatabaseTable;
  private User taskwolfUser;

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

  @Bean
  TeamWorkDatabaseTable provideTeamWorkDatabaseTable() {
    return teamWorkDatabaseTable;
  }

  @Bean
  @Qualifier("taskwolfUser")
  User provideTaskwolfUser() {
    return taskwolfUser;
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
  private void initializeTeamWorkDatabaseTable() {
    teamWorkDatabaseTable = TeamWorkDatabaseTable.create(
      databaseConnection, databaseKeyspace);
    teamWorkDatabaseTable.createIfNotExists();
  }

  @PostConstruct
  private void initializeSupportMail() throws Exception {
    var mailConfiguration = TaskwolfMailConfiguration.createAndLoad("support");
    supportMail = TaskwolfMail.create(mailConfiguration.mail(),
      mailConfiguration.smtpMailHost(), mailConfiguration.smtpMailPort(),
      mailConfiguration.imapMailHost(), mailConfiguration.imapMailPort(),
      mailConfiguration.mailUser(), mailConfiguration.mailPassword());
  }

  private static final UUID TASKWOLF_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  @PostConstruct
  private void initializeTaskwolfUser() {
    userDatabaseTable.findUserIfExists(TASKWOLF_USER_ID).thenAccept(user ->
      taskwolfUser = (user.email().equalsIgnoreCase("Unknown") ?
        insertTaskwolfUser() : user));
  }

  private User insertTaskwolfUser() {
    var password = Hashing.sha256().hashString(UUID.randomUUID().toString(),
      StandardCharsets.UTF_8).toString();
    var user = User.create(TASKWOLF_USER_ID, "Taskwolf", "support@taskwolf.net",
      password, "en", Lists.newArrayList());
    userDatabaseTable.insertUser(user);
    profilePictureDatabaseTable.insertProfilePicture(TASKWOLF_USER_ID, "");
    return user;
  }
}
