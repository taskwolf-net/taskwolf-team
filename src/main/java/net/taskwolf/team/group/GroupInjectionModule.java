package net.taskwolf.team.group;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.taskwolf.core.database.DatabaseConnection;
import net.taskwolf.core.database.DatabaseKeyspace;

@RequiredArgsConstructor(staticName = "create")
public final class GroupInjectionModule extends AbstractModule {
  @Provides
  @Singleton
  GroupDatabaseTable provideGroupDatabaseTable(
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var groupDatabaseTable = GroupDatabaseTable.create(databaseConnection,
      databaseKeyspace);
    groupDatabaseTable.createIfNotExists();
    return groupDatabaseTable;
  }
}
