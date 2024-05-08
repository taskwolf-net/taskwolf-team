package net.taskwolf.team.command;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import net.taskwolf.core.command.Command;
import net.taskwolf.core.iterator.AsyncIterator;
import net.taskwolf.core.log.Log;
import net.taskwolf.core.user.User;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import net.taskwolf.team.member.TeamMember;

import java.util.List;
import java.util.UUID;

public final class TeamCommand extends Command {
  public static TeamCommand create(
    Log log, UserDatabaseTable userDatabaseTable,
    TeamDatabaseTable teamDatabaseTable
  ) {
    return new TeamCommand(log, userDatabaseTable, teamDatabaseTable);
  }

  private final UserDatabaseTable userDatabaseTable;
  private final TeamDatabaseTable teamDatabaseTable;

  @Inject
  private TeamCommand(
    Log log, UserDatabaseTable userDatabaseTable,
    TeamDatabaseTable teamDatabaseTable
  ) {
    super(log, "team", new String[] {"member", "members", "teammember",
      "teammembers"}, new String[] {"add <id, group>", "remove <id>",
      "change <id, group>", "find <id>", "all"});
    this.userDatabaseTable = userDatabaseTable;
    this.teamDatabaseTable = teamDatabaseTable;
  }

  @Override
  public boolean execute(String[] arguments) {
    if (arguments.length == 0) {
      return false;
    }
    if (arguments[0].equalsIgnoreCase("add")) {
      return addMember(arguments);
    }
    if (arguments[0].equalsIgnoreCase("remove")) {
      return removeMember(arguments);
    }
    if (arguments[0].equalsIgnoreCase("change")) {
      return changeMemberGroup(arguments);
    }
    if (arguments[0].equalsIgnoreCase("find")) {
      return findMember(arguments);
    }
    if (arguments[0].equalsIgnoreCase("all")) {
      return allMembers(arguments);
    }
    return false;
  }

  private boolean addMember(String[] arguments) {
    if (arguments.length != 3) {
      return false;
    }
    teamDatabaseTable.insertTeamMember(TeamMember.create(UUID.fromString(arguments[1]),
      arguments[2], Lists.newArrayList()));
    log().info("Team member has been added!");
    return true;
  }

  private boolean removeMember(String[] arguments) {
    if (arguments.length != 2) {
      return false;
    }
    teamDatabaseTable.deleteTeamMember(UUID.fromString(arguments[1]));
    log().info("Team member has been removed!");
    return true;
  }

  private boolean changeMemberGroup(String[] arguments) {
    if (arguments.length != 3) {
      return false;
    }
    teamDatabaseTable.changeTeamMemberGroup(UUID.fromString(arguments[1]),
      arguments[2]);
    log().info("Team member group has been changed!");
    return true;
  }

  private boolean findMember(String[] arguments) {
    if (arguments.length != 2) {
      return false;
    }
    teamDatabaseTable.findTeamMember(UUID.fromString(arguments[1]))
      .thenAccept(member -> userDatabaseTable.findUser(member.userId())
        .thenAccept(user -> printMember(member, user)));
    return true;
  }

  private void printMember(TeamMember member, User user) {
    log().info("Team member:");
    log().info(" - Name: " + user.name());
    log().info(" - Email: " + user.email());
    log().info(" - Group: " + member.group());
    log().info(" - Permissions: ");
    for (var permission : member.permissions()) {
      log().info("   - " + permission);
    }
  }

  private boolean allMembers(String[] arguments) {
    teamDatabaseTable.findAllTeamMembers().thenAccept(this::printMembers);
    return true;
  }

  private void printMembers(List<TeamMember> members) {
    var groups = HashMultimap.<String, String>create();
    AsyncIterator.execute(members, member ->
      userDatabaseTable.findUser(member.userId()).thenAccept(user ->
        groups.put(member.group(), user.name() + " (" + user.email() + ")")))
      .thenAccept(value -> printMembers(groups));
  }

  private void printMembers(Multimap<String, String> members) {
    for (var key : members.keySet()) {
      var groupMembers = members.get(key);
      log().info(key + " (" + groupMembers.size() + ")");
      for (var member : groupMembers) {
        log().info(" - " + member);
      }
    }
  }
}
