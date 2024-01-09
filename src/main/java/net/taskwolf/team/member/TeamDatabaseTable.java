package net.taskwolf.team.member;

import com.google.common.collect.Lists;
import net.taskwolf.core.database.*;

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
    insertTeamMember(teamMember.userId(), teamMember.group(),
      teamMember.permissions());
  }

  public void insertTeamMember(UUID id, String group, List<String> permissions) {
    insert(DatabaseRow.of(id, group, permissions));
  }

  public void addTeamMemberPermission(UUID userId, String permission) {
    findTeamMember(userId).thenAccept(member ->
      addTeamMemberPermission(member, permission));
  }

  private void addTeamMemberPermission(TeamMember teamMember, String permission) {
    teamMember.addPermission(permission);
    updateTeamMember(teamMember);
  }

  public void removeTeamMemberPermission(UUID userId, String permission) {
    findTeamMember(userId).thenAccept(member ->
      removeTeamMemberPermission(member, permission));
  }

  private void removeTeamMemberPermission(TeamMember teamMember, String permission) {
    teamMember.removePermission(permission);
    updateTeamMember(teamMember);
  }

  public void changeTeamMemberGroup(UUID userId, String group) {
    findTeamMember(userId).thenAccept(member ->
      changeTeamMemberGroup(member, group));
  }

  private void changeTeamMemberGroup(TeamMember teamMember, String group) {
    teamMember.changeGroup(group);
    updateTeamMember(teamMember);
  }

  private void updateTeamMember(TeamMember teamMember) {
    update(DatabaseCell.create(teamMember.userId()), DatabaseRow.of(
      teamMember.userId(), teamMember.group(), teamMember.permissions()));
  }

  public void deleteTeamMember(UUID id) {
    delete(DatabaseCell.create(id));
  }

  public CompletableFuture<Boolean> teamMemberExists(UUID id) {
    return exists(DatabaseCell.create(id));
  }

  public CompletableFuture<TeamMember> findTeamMember(UUID id) {
    return selectRow(DatabaseCell.create(id)).thenApply(TeamMember::of);
  }

  public CompletableFuture<List<TeamMember>> findAllTeamMembers() {
    return selectAllRows().thenApply(rows ->
      rows.stream().map(TeamMember::of).collect(Collectors.toList()));
  }
}

