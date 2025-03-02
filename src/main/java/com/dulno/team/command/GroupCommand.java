package com.dulno.team.command;

import com.dulno.core.command.Command;
import com.dulno.core.log.Log;
import com.dulno.team.group.Group;
import com.dulno.team.group.GroupDatabaseTable;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import java.util.List;

public final class GroupCommand extends Command {
  private final GroupDatabaseTable groupDatabaseTable;

  @Inject
  private GroupCommand(Log log, GroupDatabaseTable groupDatabaseTable) {
    super(log, "group", new String[] {"groups"}, new String[] {"add <name, priority>",
      "remove <name>", "find <name>", "all"});
    this.groupDatabaseTable = groupDatabaseTable;
  }

  @Override
  public boolean execute(String[] arguments) {
    if (arguments.length == 0) {
      return false;
    }
    if (arguments[0].equalsIgnoreCase("add")) {
      return addGroup(arguments);
    }
    if (arguments[0].equalsIgnoreCase("remove")) {
      return removeGroup(arguments);
    }
    if (arguments[0].equalsIgnoreCase("find")) {
      return findGroup(arguments);
    }
    if (arguments[0].equalsIgnoreCase("all")) {
      return allGroups(arguments);
    }
    return false;
  }

  private boolean addGroup(String[] arguments) {
    if (arguments.length != 3) {
      return false;
    }
    groupDatabaseTable.insertGroup(Group.create(arguments[1],
      Integer.parseInt(arguments[2]), "#ffffff", Lists.newArrayList()));
    log().info("Group has been added!");
    return true;
  }

  private boolean removeGroup(String[] arguments) {
    if (arguments.length != 2) {
      return false;
    }
    groupDatabaseTable.deleteGroup(arguments[1]);
    log().info("Group has been removed!");
    return true;
  }

  private boolean findGroup(String[] arguments) {
    if (arguments.length != 2) {
      return false;
    }
    groupDatabaseTable.findGroup(arguments[1]).thenAccept(this::printGroup);
    return true;
  }

  private void printGroup(Group group) {
    log().info("Group:");
    log().info(" - Name: " + group.name());
    log().info(" - Permissions: ");
    for (var permission : group.permissions()) {
      log().info("   - " + permission);
    }
  }

  private boolean allGroups(String[] arguments) {
    groupDatabaseTable.findAllGroups().thenAccept(this::printGroups);
    return true;
  }

  private void printGroups(List<Group> groups) {
    log().info("Groups: ");
    for (var group : groups) {
      log().info(" - " + group.name());
    }
  }
}
