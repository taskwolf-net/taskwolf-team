package net.taskwolf.team.mail;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.taskwolf.core.database.DatabaseConnection;
import net.taskwolf.core.database.DatabaseKeyspace;

@RequiredArgsConstructor(staticName = "create")
public final class SupportMailInjectionModule extends AbstractModule {
  @Provides
  @Singleton
  SupportMailDatabaseTable provideSupportMailDatabaseTable(
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var supportMailDatabaseTable = SupportMailDatabaseTable.create(
      databaseConnection, databaseKeyspace);
    supportMailDatabaseTable.createIfNotExists();
    return supportMailDatabaseTable;
  }

  @Provides
  @Singleton
  SupportMailMessageDatabaseTable provideSupportMailMessageDatabaseTable(
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var supportMailMessageDatabaseTable = SupportMailMessageDatabaseTable.create(
      databaseConnection, databaseKeyspace);
    supportMailMessageDatabaseTable.createIfNotExists();
    return supportMailMessageDatabaseTable;
  }
}
