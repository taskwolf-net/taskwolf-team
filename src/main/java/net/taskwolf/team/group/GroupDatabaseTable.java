package net.taskwolf.team.group;

import com.google.common.collect.Lists;
import net.taskwolf.core.database.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class GroupDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "group";

  public static GroupDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("name", DatabaseDataType.TEXT,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseColumn.create("priority", DatabaseDataType.INT));
    columns.add(DatabaseListColumn.create("permissions", DatabaseDataType.TEXT));
    return new GroupDatabaseTable(connection, keyspace, TABLE_NAME, columns);
  }

  private GroupDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public void insertGroup(Group group) {
    insertGroup(group.name(), group.priority(), group.permissions());
  }

  public void insertGroup(String name, int priority, List<String> permissions) {
    insert(DatabaseRow.of(name, priority, permissions));
  }

  public void addGroupPermission(String name, String permission) {
    findGroup(name).thenAccept(group ->
      addGroupPermission(group, permission));
  }

  private void addGroupPermission(Group group, String permission) {
    group.addPermission(permission);
    updateGroup(group);
  }

  public void removeGroupPermission(String name, String permission) {
    findGroup(name).thenAccept(group ->
      removeGroupPermission(group, permission));
  }

  private void removeGroupPermission(Group group, String permission) {
    group.removePermission(permission);
    updateGroup(group);
  }

  public void changeGroupPriority(String name, int priority) {
    findGroup(name).thenAccept(group ->
      changeGroupPriority(group, priority));
  }

  private void changeGroupPriority(Group group, int priority) {
    group.changePriority(priority);
    updateGroup(group);
  }

  public void renameGroup(String name, String newName) {
    findGroup(name).thenAccept(group -> renameGroup(group, newName));
  }

  private void renameGroup(Group group, String newName) {
    deleteGroup(group.name());
    group.rename(newName);
    insertGroup(group);
  }

  private void updateGroup(Group group) {
    update(group.name(), DatabaseRow.of(group.name(),
      group.priority(), group.permissions()));
  }

  public void deleteGroup(String name) {
    delete(name);
  }

  public CompletableFuture<Boolean> groupExists(String name) {
    return exists(name);
  }

  public CompletableFuture<Group> findGroup(String name) {
    return selectRow(name).thenApply(Group::of);
  }

  public CompletableFuture<List<Group>> findAllGroups() {
    return selectAllRows().thenApply(rows ->
      rows.stream().map(Group::of).collect(Collectors.toList()));
  }
}
