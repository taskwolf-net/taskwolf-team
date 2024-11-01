package com.dulno.team.member.mfa;

import com.dulno.core.database.*;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TeamMultiFactorAuthDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "team_multi_factor_auth";

  public static TeamMultiFactorAuthDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("member", DatabaseDataType.UUID,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseColumn.create("secret", DatabaseDataType.TEXT));
    columns.add(DatabaseListColumn.create("recoveryCodes", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("confirmed", DatabaseDataType.BOOLEAN));
    return new TeamMultiFactorAuthDatabaseTable(connection, keyspace, TABLE_NAME, columns);
  }

  private TeamMultiFactorAuthDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public CompletableFuture<Void> insertAuth(
    UUID memberId, String secret, List<String> recoveryCodes
  ) {
    return insert(DatabaseRow.of(memberId, secret, recoveryCodes, false),
      "USING TTL " + (60 * 60));
  }

  public CompletableFuture<Void> confirmAuth(UUID memberId) {
    return findAuth(memberId).thenCompose(auth ->
      deleteAuth(memberId).thenCompose(value ->
        insert(DatabaseRow.of(memberId, auth.secret(), auth.recoveryCodes(), true))));
  }

  public CompletableFuture<Boolean> authExists(UUID memberId) {
    return exists(memberId);
  }

  public CompletableFuture<Void> deleteAuth(UUID memberId) {
    return delete(memberId);
  }

  public CompletableFuture<TeamMultiFactorAuthUser> findAuth(UUID memberId) {
    return selectRow(memberId).thenApply(TeamMultiFactorAuthUser::of);
  }
}
