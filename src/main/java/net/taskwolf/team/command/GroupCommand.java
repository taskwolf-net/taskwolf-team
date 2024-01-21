package net.taskwolf.team.command;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import net.taskwolf.core.command.Command;
import net.taskwolf.core.log.Log;
import net.taskwolf.team.group.Group;
import net.taskwolf.team.group.GroupDatabaseTable;

import java.util.List;

public final class GroupCommand extends Command {
  public static GroupCommand create(Log log, GroupDatabaseTable groupDatabaseTable) {
    return new GroupCommand(log, groupDatabaseTable);
  }

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
      Integer.parseInt(arguments[2]), Lists.newArrayList()));
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
