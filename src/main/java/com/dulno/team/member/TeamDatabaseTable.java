package com.dulno.team.member;

import com.dulno.core.database.*;
import com.dulno.core.database.condition.DatabaseCondition;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class TeamDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "team";

  public static TeamDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("id", DatabaseDataType.UUID,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseColumn.create("name", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("email", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("passwordHash", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("language", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("group", DatabaseDataType.TEXT));
    columns.add(DatabaseListColumn.create("permissions", DatabaseDataType.TEXT));
    return new TeamDatabaseTable(connection, keyspace, TABLE_NAME, columns);
  }

  private TeamDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public void insertTeamMember(TeamMember teamMember) {
    insertTeamMember(teamMember.id(), teamMember.name(), teamMember.email(),
      teamMember.passwordHash(), teamMember.language(), teamMember.group(),
      teamMember.permissions());
  }

  public void insertTeamMember(
    UUID id, String name, String email, String passwordHash, String language,
    String group, List<String> permissions
  ) {
    insert(DatabaseRow.of(id, name, email, passwordHash, language, group,
      permissions));
  }

  public void changeTeamMemberName(UUID memberId, String name) {
    findTeamMember(memberId).thenAccept(member ->
      changeTeamMemberName(member, name));
  }

  public void changeTeamMemberName(TeamMember teamMember, String name) {
    teamMember.changeName(name);
    updateTeamMember(teamMember);
  }

  public void changeTeamMemberEmail(UUID memberId, String email) {
    findTeamMember(memberId).thenAccept(member ->
      changeTeamMemberEmail(member, email));
  }

  public void changeTeamMemberEmail(TeamMember teamMember, String email) {
    teamMember.changeEmail(email);
    updateTeamMember(teamMember);
  }

  public void changeTeamMemberPassword(UUID memberId, String passwordHash) {
    findTeamMember(memberId).thenAccept(member ->
      changeTeamMemberPassword(member, passwordHash));
  }

  public void changeTeamMemberPassword(TeamMember teamMember, String passwordHash) {
    teamMember.changePassword(passwordHash);
    updateTeamMember(teamMember);
  }

  public void changeTeamMemberLanguage(UUID memberId, String language) {
    findTeamMember(memberId).thenAccept(member ->
      changeTeamMemberLanguage(member, language));
  }

  public void changeTeamMemberLanguage(TeamMember teamMember, String language) {
    teamMember.changeLanguage(language);
    updateTeamMember(teamMember);
  }

  public void addTeamMemberPermission(UUID memberId, String permission) {
    findTeamMember(memberId).thenAccept(member ->
      addTeamMemberPermission(member, permission));
  }

  public void addTeamMemberPermission(TeamMember teamMember, String permission) {
    teamMember.addPermission(permission);
    updateTeamMember(teamMember);
  }

  public void removeTeamMemberPermission(UUID memberId, String permission) {
    findTeamMember(memberId).thenAccept(member ->
      removeTeamMemberPermission(member, permission));
  }

  public void removeTeamMemberPermission(TeamMember teamMember, String permission) {
    teamMember.removePermission(permission);
    updateTeamMember(teamMember);
  }

  public void changeTeamMemberGroup(UUID memberId, String group) {
    findTeamMember(memberId).thenAccept(member ->
      changeTeamMemberGroup(member, group));
  }

  public void changeTeamMemberGroup(TeamMember teamMember, String group) {
    teamMember.changeGroup(group);
    updateTeamMember(teamMember);
  }

  private void updateTeamMember(TeamMember teamMember) {
    update(teamMember.id(), DatabaseRow.of(teamMember.id(),
      teamMember.name(), teamMember.email(), teamMember.passwordHash(),
      teamMember.language(), teamMember.group(), teamMember.permissions()));
  }

  public void deleteTeamMember(UUID id) {
    delete(id);
  }

  public CompletableFuture<UUID> generateAvailableMemberId() {
    var futureResponse = new CompletableFuture<UUID>();
    var id = UUID.randomUUID();
    teamMemberExists(id).thenApply(exists -> exists ?
      generateAvailableMemberId().thenApply(futureResponse::complete) :
      CompletableFuture.completedFuture(futureResponse.complete(id)));
    return futureResponse;
  }

  public CompletableFuture<Boolean> teamMemberExists(UUID id) {
    return exists(id);
  }

  public CompletableFuture<Boolean> teamMemberExists(String email) {
    return exists(DatabaseCondition.of("email", email.toLowerCase()));
  }

  public CompletableFuture<TeamMember> findTeamMember(UUID id) {
    return selectRow(id).thenApply(TeamMember::of);
  }

  public CompletableFuture<TeamMember> findTeamMember(String email) {
    return selectRow(DatabaseCondition.of("email", email.toLowerCase()))
      .thenApply(TeamMember::of);
  }

  public CompletableFuture<List<TeamMember>> findTeamMembersOfGroup(String group) {
    return selectRows(DatabaseCondition.of("group", group)).thenApply(rows ->
      rows.stream().map(TeamMember::of).collect(Collectors.toList()));
  }

  public CompletableFuture<List<TeamMember>> findAllTeamMembers() {
    return selectAllRows().thenApply(rows ->
      rows.stream().map(TeamMember::of).collect(Collectors.toList()));
  }
}

