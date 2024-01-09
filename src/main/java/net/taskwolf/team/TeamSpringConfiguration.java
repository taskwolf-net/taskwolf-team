package net.taskwolf.team;

import jakarta.annotation.PostConstruct;
import net.taskwolf.core.database.DatabaseConnection;
import net.taskwolf.core.database.DatabaseKeyspace;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Bean
  TeamDatabaseTable provideTeamDatabaseTable() {
    return teamDatabaseTable;
  }

  @Bean
  GroupDatabaseTable provideGroupDatabaseTable() {
    return groupDatabaseTable;
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
}
