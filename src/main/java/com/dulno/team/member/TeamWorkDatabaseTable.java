package com.dulno.team.member;

import com.dulno.core.database.*;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TeamWorkDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "team_work";

  public static TeamWorkDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("id", DatabaseDataType.UUID,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseListColumn.create("requestReplies", DatabaseDataType.BIGINT));
    return new TeamWorkDatabaseTable(connection, keyspace, TABLE_NAME, columns);
  }

  private TeamWorkDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public void insertRequestReplies(UUID id, List<Long> requestReplies) {
    insert(DatabaseRow.of(id, requestReplies));
  }

  public void addRequestReply(UUID id, long replyTime) {
    findRequestReplies(id).thenAccept(replies ->
      addRequestReply(id, replies, replyTime));
  }

  private void addRequestReply(
    UUID id, List<Long> requestReplies, long replyTime
  ) {
    requestReplies.add(replyTime);
    updateRequestReplies(id, requestReplies);
  }

  public void removeRequestReply(UUID id, long replyTime) {
    findRequestReplies(id).thenAccept(replies ->
      removeRequestReply(id, replies, replyTime));
  }

  private void removeRequestReply(
    UUID id, List<Long> requestReplies, long replyTime
  ) {
    requestReplies.remove(replyTime);
    updateRequestReplies(id, requestReplies);
  }

  private void updateRequestReplies(UUID id, List<Long> requestReplies) {
    update(id, DatabaseRow.of(id, requestReplies));
  }

  public void deleteRequestReplies(UUID id) {
    delete(id);
  }

  public CompletableFuture<Boolean> hasRequestReplies(UUID id) {
    return exists(id);
  }

  public CompletableFuture<List<Long>> findRequestReplies(UUID id) {
    return selectRow(id).thenApply(row -> row.findCell(1).listValue());
  }
}
