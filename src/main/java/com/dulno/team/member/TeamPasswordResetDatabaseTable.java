package com.dulno.team.member;

import com.dulno.core.database.*;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TeamPasswordResetDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "team_password_reset";

  public static TeamPasswordResetDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("id", DatabaseDataType.UUID,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseColumn.create("resetToken", DatabaseDataType.TEXT));
    return new TeamPasswordResetDatabaseTable(connection, keyspace, TABLE_NAME, columns);
  }

  private TeamPasswordResetDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public void insertResetToken(UUID memberId, String token) {
    insert(DatabaseRow.of(memberId, token));
  }

  public CompletableFuture<Boolean> resetTokenExists(UUID memberId) {
    return exists(memberId);
  }

  public void deleteResetToken(UUID memberId) {
    delete(memberId);
  }

  public CompletableFuture<String> findResetToken(UUID memberId) {
    return selectRow(memberId).thenApply(row -> row.findCell(1).stringValue());
  }
}
