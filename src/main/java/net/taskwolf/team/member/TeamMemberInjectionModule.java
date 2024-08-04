package net.taskwolf.team.member;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.taskwolf.core.database.DatabaseConnection;
import net.taskwolf.core.database.DatabaseKeyspace;

@RequiredArgsConstructor(staticName = "create")
public final class TeamMemberInjectionModule extends AbstractModule {
  @Provides
  @Singleton
  TeamDatabaseTable provideTeamDatabaseTable(
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var teamDatabaseTable = TeamDatabaseTable.create(databaseConnection,
      databaseKeyspace);
    teamDatabaseTable.createIfNotExists();
    return teamDatabaseTable;
  }

  @Provides
  @Singleton
  TeamVerificationDatabaseTable provideTeamVerificationDatabaseTable(
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var teamVerificationDatabaseTable = TeamVerificationDatabaseTable.create(
      databaseConnection, databaseKeyspace);
    teamVerificationDatabaseTable.createIfNotExists();
    return teamVerificationDatabaseTable;
  }

  @Provides
  @Singleton
  TeamWorkDatabaseTable provideTeamWorkDatabaseTable(
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var teamWorkDatabaseTable = TeamWorkDatabaseTable.create(databaseConnection,
      databaseKeyspace);
    teamWorkDatabaseTable.createIfNotExists();
    return teamWorkDatabaseTable;
  }
}
