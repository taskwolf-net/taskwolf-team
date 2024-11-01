package com.dulno.team.member;

import com.dulno.core.database.*;
import com.google.common.collect.Lists;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TeamEmailChangeDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "team_email_change";

  public static TeamEmailChangeDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("id", DatabaseDataType.UUID,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseColumn.create("newEmail", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("changeToken", DatabaseDataType.TEXT));
    return new TeamEmailChangeDatabaseTable(connection, keyspace, TABLE_NAME, columns);
  }

  private TeamEmailChangeDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public void insertChange(UUID memberId, String newEmail, String token) {
    insert(DatabaseRow.of(memberId, newEmail, token));
  }

  public void updateChange(UUID memberId, String newEmail, String token) {
    update(memberId, DatabaseRow.of(memberId, newEmail, token));
  }

  public CompletableFuture<Boolean> changeExists(UUID memberId) {
    return exists(memberId);
  }

  public void deleteChange(UUID memberId) {
    delete(memberId);
  }

  public CompletableFuture<Map.Entry<String, String>> findChange(UUID memberId) {
    return selectRow(memberId).thenApply(row ->
      new AbstractMap.SimpleEntry<>(row.findCell(1).stringValue(),
        row.findCell(2).stringValue()));
  }
}
