package net.taskwolf.team.command;

import net.taskwolf.core.command.Command;
import net.taskwolf.core.log.Log;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import com.google.inject.Inject;

import java.util.UUID;

public final class PermissionCommand extends Command {
  private final TeamDatabaseTable teamDatabaseTable;
  private final GroupDatabaseTable groupDatabaseTable;

  @Inject
  private PermissionCommand(
    Log log, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable
  ) {
    super(log, "permission", new String[] {"permissions"}, new String[] {
      "user add <id, permission>", "user remove <id, permission>",
      "group add <name, permission>", "group remove <name, permission>"});
    this.teamDatabaseTable = teamDatabaseTable;
    this.groupDatabaseTable = groupDatabaseTable;
  }

  @Override
  public boolean execute(String[] arguments) {
    if (arguments.length == 0) {
      return false;
    }
    if (arguments[0].equalsIgnoreCase("user")) {
      return userPermission(arguments);
    }
    if (arguments[0].equalsIgnoreCase("group")) {
      return groupPermission(arguments);
    }
    return false;
  }

  private boolean userPermission(String[] arguments) {
    if (arguments.length < 2) {
      return false;
    }
    if (arguments[1].equalsIgnoreCase("add")) {
      return addUserPermission(arguments);
    }
    if (arguments[1].equalsIgnoreCase("remove")) {
      return removeUserPermission(arguments);
    }
    return true;
  }

  private boolean addUserPermission(String[] arguments) {
    if (arguments.length != 4) {
      return false;
    }
    teamDatabaseTable.addTeamMemberPermission(UUID.fromString(arguments[2]),
      arguments[3]);
    log().info("Used permission added!");
    return true;
  }

  private boolean removeUserPermission(String[] arguments) {
    if (arguments.length != 4) {
      return false;
    }
    teamDatabaseTable.removeTeamMemberPermission(UUID.fromString(arguments[2]),
      arguments[3]);
    log().info("Used permission removed!");
    return true;
  }

  private boolean groupPermission(String[] arguments) {
    if (arguments.length < 2) {
      return false;
    }
    if (arguments[1].equalsIgnoreCase("add")) {
      return addGroupPermission(arguments);
    }
    if (arguments[1].equalsIgnoreCase("remove")) {
      return removeGroupPermission(arguments);
    }
    return true;
  }

  private boolean addGroupPermission(String[] arguments) {
    if (arguments.length != 4) {
      return false;
    }
    groupDatabaseTable.addGroupPermission(arguments[2], arguments[3]);
    log().info("Group permission added!");
    return true;
  }

  private boolean removeGroupPermission(String[] arguments) {
    if (arguments.length != 4) {
      return false;
    }
    groupDatabaseTable.removeGroupPermission(arguments[2], arguments[3]);
    log().info("Group permission removed!");
    return true;
  }
}
