package com.dulno.team.member.session;

import com.dulno.core.database.*;
import com.dulno.core.database.condition.DatabaseCondition;
import com.dulno.core.session.SessionStatus;
import com.google.common.collect.Lists;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TeamSessionDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "team_session";

  public static TeamSessionDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("id", DatabaseDataType.UUID,
      DatabaseColumn.Type.PARTITION_KEY));
    columns.add(DatabaseColumn.create("member", DatabaseDataType.UUID,
      DatabaseColumn.Type.CLUSTERING_KEY));
    columns.add(DatabaseColumn.create("status", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("devicePlatform", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("ipAddress", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("country", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("city", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("openTime", DatabaseDataType.BIGINT));
    columns.add(DatabaseColumn.create("refreshToken", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("lastRefresh", DatabaseDataType.BIGINT));
    var table = new TeamSessionDatabaseTable(connection, keyspace, TABLE_NAME, columns);
    table.createIfNotExists();
    table.createIndexIfNotExists("member");
    table.createIndexIfNotExists("status");
    table.initializeViews();
    return table;
  }

  private DatabaseTable memberStatusView;

  private TeamSessionDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  private void initializeViews() {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("member", DatabaseDataType.UUID,
      DatabaseColumn.Type.PARTITION_KEY));
    columns.add(DatabaseColumn.create("status", DatabaseDataType.TEXT,
      DatabaseColumn.Type.CLUSTERING_KEY));
    columns.add(DatabaseColumn.create("id", DatabaseDataType.UUID,
      DatabaseColumn.Type.CLUSTERING_KEY));
    memberStatusView = createMaterializedViewIfNotExists("member_status_view",
      columns);
  }

  public CompletableFuture<Void> insertSession(TeamSession session) {
    return insertSession(session.id(), session.memberId(), session.status(),
      session.devicePlatform(), session.ipAddress(), session.country(),
      session.city(), session.openTime(), session.lastRefreshToken(),
      session.lastRefresh());
  }

  public CompletableFuture<Void> insertSession(
    UUID id, UUID memberId, SessionStatus status, String devicePlatform,
    String ipAddress, String country, String city, long openTime,
    String refreshToken, long lastRefresh
  ) {
    return insert(DatabaseRow.of(id, memberId, status.toString(), devicePlatform,
      ipAddress, country, city, openTime, refreshToken, lastRefresh));
  }

  public CompletableFuture<Void> updateSessionRefreshToken(
    UUID id, String refreshToken
  ) {
    var futureResponse = new CompletableFuture<Void>();
    findSession(id)
      .thenAccept(session -> updateSessionRefreshToken(session, refreshToken)
      .thenAccept(futureResponse::complete));
    return futureResponse;
  }

  public CompletableFuture<Void> updateSessionRefreshToken(
    TeamSession session, String refreshToken
  ) {
    session.updateRefreshToken(refreshToken);
    return updateSession(session);
  }

  public CompletableFuture<Void> closeSession(UUID id) {
    var futureResponse = new CompletableFuture<Void>();
    findSession(id).thenAccept(session -> closeSession(session)
      .thenAccept(futureResponse::complete));
    return futureResponse;
  }

  public CompletableFuture<Void> closeSession(TeamSession session) {
    session.close();
    return updateSession(session);
  }

  public CompletableFuture<Void> updateSession(TeamSession session) {
    return update(DatabaseCondition.of("id", session.id(), "member", session.memberId()),
      DatabaseRow.of(session.id(), session.memberId(), session.status().toString(),
        session.devicePlatform(), session.ipAddress(), session.country(),
        session.city(), session.openTime(), session.lastRefreshToken(),
        session.lastRefresh()));
  }

  public CompletableFuture<Void> deleteSession(UUID id) {
    return delete(DatabaseCondition.of("id", id));
  }

  public CompletableFuture<UUID> generateAvailableSessionId() {
    var futureResponse = new CompletableFuture<UUID>();
    var id = UUID.randomUUID();
    sessionExists(id).thenApply(exists -> exists ?
      generateAvailableSessionId().thenApply(futureResponse::complete) :
      CompletableFuture.completedFuture(futureResponse.complete(id)));
    return futureResponse;
  }

  public CompletableFuture<Boolean> sessionExists(UUID id) {
    return exists(DatabaseCondition.of("id", id));
  }

  public CompletableFuture<TeamSession> findSession(UUID id) {
    return selectRow(DatabaseCondition.of("id", id))
      .thenApply(row -> TeamSession.of(row, this));
  }

  public CompletableFuture<List<TeamSession>> findSessionsOfMember(UUID memberId) {
    return selectRows(DatabaseCondition.of("member", memberId))
      .thenApply(rows -> rows.stream().map(row -> TeamSession.of(row, this)).toList());
  }

  public CompletableFuture<List<TeamSession>> findSessionsOfMemberByStatus(
    UUID memberId, SessionStatus status
  ) {
    var condition = DatabaseCondition.of("member", memberId, "status",
      status.toString());
    return memberStatusView.selectRows(condition).thenApply(rows ->
      rows.stream().map(row -> TeamSession.of(row, memberStatusView))
        .sorted(Comparator.comparingLong(TeamSession::openTime).reversed()).toList());
  }

  public CompletableFuture<List<TeamSession>> findAllSessionsByStatus(
    SessionStatus status
  ) {
    return selectRows(DatabaseCondition.of("status", status.toString()))
      .thenApply(rows -> rows.stream().map(row -> TeamSession.of(row, this)).toList());
  }
}