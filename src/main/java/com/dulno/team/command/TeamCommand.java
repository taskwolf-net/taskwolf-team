package com.dulno.team.command;

import com.dulno.core.command.Command;
import com.dulno.core.environment.DulnoEnvironment;
import com.dulno.core.log.Log;
import com.dulno.core.mail.Mail;
import com.dulno.team.member.TeamDatabaseTable;
import com.dulno.team.member.TeamMember;
import com.dulno.team.member.TeamVerificationDatabaseTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.List;
import java.util.UUID;

public final class TeamCommand extends Command {
  private final TeamDatabaseTable teamDatabaseTable;
  private final TeamVerificationDatabaseTable teamVerificationDatabaseTable;
  private final Mail verificationMail;
  private final DulnoEnvironment environment;

  @Inject
  private TeamCommand(
    Log log, TeamDatabaseTable teamDatabaseTable,
    TeamVerificationDatabaseTable teamVerificationDatabaseTable,
    @Named("verificationMail") Mail verificationMail,
    DulnoEnvironment environment
  ) {
    super(log, "team", new String[] {"member", "members", "teammember",
      "teammembers"}, new String[] {"add <name, email, group>", "remove <id>",
      "change <id, group>", "find <id>", "all"});
    this.teamDatabaseTable = teamDatabaseTable;
    this.teamVerificationDatabaseTable = teamVerificationDatabaseTable;
    this.verificationMail = verificationMail;
    this.environment = environment;
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
    if (arguments.length != 4) {
      return false;
    }
    teamDatabaseTable.generateAvailableMemberId().thenAccept(id ->
      addMember(id, arguments[1], arguments[2], arguments[3]));
    log().info("Team member has been added!");
    return true;
  }

  private static final String ONBOARDING_EMAIL_TITLE = "Onboarding";
  private static final String ONBOARDING_URL = "https://panel.%s/confirm/%s/%s/";
  private static final String ONBOARDING_EMAIL_BODY = "Hey %s,\n" +
    "\n" +
    "welcome to the Dulno team. Here's to good cooperation! " +
    "In the following you will get access to the Dulno-Panel. " +
    "Please follow the steps below.\n" +
    "\n" +
    "Click the link below and set your panel password:\n" +
    "%s\n" +
    "\n" +
    "That's it already.\n" +
    "\n" +
    "Welcome aboard!";

  private void addMember(
    UUID id, String name, String email, String group
  ) {
    name = name.replaceAll("_", " ");
    teamDatabaseTable.insertTeamMember(id, name, email, "", "en",
      group, Lists.newArrayList());
    var token = UUID.randomUUID().toString();
    teamVerificationDatabaseTable.insertVerification(id, token);
    var body = String.format(ONBOARDING_EMAIL_BODY, name,
      String.format(ONBOARDING_URL, environment.domain(), id.toString(), token));
    verificationMail.send(email, "en", ONBOARDING_EMAIL_TITLE, body);
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
      .thenAccept(this::printMember);
    return true;
  }

  private void printMember(TeamMember member) {
    log().info("Team member:");
    log().info(" - Name: " + member.name());
    log().info(" - Email: " + member.email());
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
    var groups = HashMultimap.<String, TeamMember>create();
    for (var member : members) {
      groups.put(member.group(), member);
    }
    printMembers(groups);
  }

  private void printMembers(Multimap<String, TeamMember> members) {
    for (var key : members.keySet()) {
      var groupMembers = members.get(key);
      log().info(key + " (" + groupMembers.size() + ")");
      for (var member : groupMembers) {
        log().info(" - " + (member.name() + " (" + member.email() + ") (" + member.id() + ")"));
      }
    }
  }
}
