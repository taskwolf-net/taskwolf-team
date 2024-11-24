package com.dulno.team.member;

import com.dulno.core.database.DatabaseConnection;
import com.dulno.core.database.DatabaseKeyspace;
import com.dulno.team.member.mfa.TeamMultiFactorAuthDatabaseTable;
import com.dulno.team.member.session.TeamSessionDatabaseTable;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.maxmind.geoip2.DatabaseReader;
import lombok.RequiredArgsConstructor;

import java.io.File;

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
    teamDatabaseTable.createIndexIfNotExists("email");
    teamDatabaseTable.createIndexIfNotExists("group");
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
  TeamEmailChangeDatabaseTable provideTeamEmailChangeDatabaseTable(
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var teamEmailChangeDatabaseTable = TeamEmailChangeDatabaseTable.create(
      databaseConnection, databaseKeyspace);
    teamEmailChangeDatabaseTable.createIfNotExists();
    return teamEmailChangeDatabaseTable;
  }

  @Provides
  @Singleton
  TeamPasswordResetDatabaseTable provideTeamPasswordResetDatabaseTable(
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var teamPasswordResetDatabaseTable = TeamPasswordResetDatabaseTable.create(
      databaseConnection, databaseKeyspace);
    teamPasswordResetDatabaseTable.createIfNotExists();
    return teamPasswordResetDatabaseTable;
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

  @Provides
  @Singleton
  TeamMultiFactorAuthDatabaseTable provideTeamMultiFactorAuthDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var teamMultiFactorAuthDatabaseTable = TeamMultiFactorAuthDatabaseTable.create(
      connection, keyspace);
    teamMultiFactorAuthDatabaseTable.createIfNotExists();
    return teamMultiFactorAuthDatabaseTable;
  }

  @Provides
  @Singleton
  TeamSessionDatabaseTable provideTeamSessionDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
   return TeamSessionDatabaseTable.create(connection, keyspace);
  }
}
