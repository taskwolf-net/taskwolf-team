package com.dulno.team.access.member;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.environment.DulnoEnvironment;
import com.dulno.core.mail.Mail;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.*;
import com.dulno.team.member.mfa.TeamMultiFactorAuthDatabaseTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class MemberController extends TeamController {
  private final TeamVerificationDatabaseTable teamVerificationDatabaseTable;
  private final TeamPasswordResetDatabaseTable teamPasswordResetDatabaseTable;
  private final TeamEmailChangeDatabaseTable teamEmailChangeDatabaseTable;
  private final TeamMultiFactorAuthDatabaseTable teamMultiFactorAuthDatabaseTable;
  private final Mail verificationMail;
  private final DulnoEnvironment environment;

  private MemberController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    TeamVerificationDatabaseTable teamVerificationDatabaseTable,
    TeamPasswordResetDatabaseTable teamPasswordResetDatabaseTable,
    TeamEmailChangeDatabaseTable teamEmailChangeDatabaseTable,
    TeamMultiFactorAuthDatabaseTable teamMultiFactorAuthDatabaseTable,
    @Qualifier("verificationMail") Mail verificationMail,
    DulnoEnvironment environment
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.teamVerificationDatabaseTable = teamVerificationDatabaseTable;
    this.teamPasswordResetDatabaseTable = teamPasswordResetDatabaseTable;
    this.teamEmailChangeDatabaseTable = teamEmailChangeDatabaseTable;
    this.teamMultiFactorAuthDatabaseTable = teamMultiFactorAuthDatabaseTable;
    this.verificationMail = verificationMail;
    this.environment = environment;
  }

  @RequestMapping(path = "/member/add/", method = RequestMethod.POST)
  public void addMember(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    memberHasPermission(findMemberId(request), "member.add").thenAccept(
      hasPermission -> addMember(body.getString("name"), body.getString("email"),
        body.getString("group"), hasPermission));
  }

  private void addMember(
    String name, String email, String group, boolean hasPermission
  ) {
    if (!hasPermission) {
      return;
    }
    teamDatabaseTable().generateAvailableMemberId()
      .thenAccept(id -> addMember(id, name, email, group));
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
    teamDatabaseTable().insertTeamMember(id, name, email, "", "en",
      group, Lists.newArrayList());
    var token = UUID.randomUUID().toString();
    teamVerificationDatabaseTable.insertVerification(id, token);
    var body = String.format(ONBOARDING_EMAIL_BODY, name,
      String.format(ONBOARDING_URL, environment.domain(), id.toString(), token));
    verificationMail.send(email, "en", ONBOARDING_EMAIL_TITLE, body);
  }

  @RequestMapping(path = "/member/remove/", method = RequestMethod.POST)
  public void removeMember(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var memberId = body.getUUID("member");
    memberHasPermission(findMemberId(request), "member.remove").thenAccept(
      hasPermission -> teamDatabaseTable().teamMemberExists(memberId).thenAccept(
        memberExists -> removeMember(memberId, hasPermission, memberExists)));
  }

  private void removeMember(
    UUID memberId, boolean hasPermission, boolean memberExists
  ) {
    if (!hasPermission || !memberExists) {
      return;
    }
    teamDatabaseTable().deleteTeamMember(memberId);
    teamPasswordResetDatabaseTable.deleteResetToken(memberId);
    teamEmailChangeDatabaseTable.deleteChange(memberId);
    teamMultiFactorAuthDatabaseTable.deleteAuth(memberId);
  }

  @RequestMapping(path = "/member/group/change/", method = RequestMethod.POST)
  public void changeMemberGroup(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var memberId = body.getUUID("member");
    memberHasPermission(findMemberId(request), "member.group.change")
      .thenAccept(hasPermission -> teamDatabaseTable().teamMemberExists(memberId)
        .thenAccept(memberExists -> changeMemberGroup(memberId,
          body.getString("group"), hasPermission, memberExists)));
  }

  private void changeMemberGroup(
    UUID memberId, String group, boolean hasPermission, boolean memberExists
  ) {
    if (!hasPermission || !memberExists) {
      return;
    }
    teamDatabaseTable().changeTeamMemberGroup(memberId, group);
  }

  @RequestMapping(path = "/member/permission/add/", method = RequestMethod.POST)
  public void addMemberPermission(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var memberId = body.getUUID("member");
    memberHasPermission(findMemberId(request), "member.permission.add")
      .thenAccept(hasPermission -> teamDatabaseTable().teamMemberExists(memberId)
        .thenAccept(memberExists -> addMemberPermission(memberId,
          body.getString("permission"), hasPermission, memberExists)));
  }

  private void addMemberPermission(
    UUID memberId, String permission, boolean hasPermission, boolean memberExists
  ) {
    if (!hasPermission || !memberExists) {
      return;
    }
    teamDatabaseTable().addTeamMemberPermission(memberId, permission);
  }

  @RequestMapping(path = "/member/permission/remove/", method = RequestMethod.POST)
  public void removeMemberPermission(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var memberId = body.getUUID("member");
    memberHasPermission(findMemberId(request), "member.permission.remove")
      .thenAccept(hasPermission -> teamDatabaseTable().teamMemberExists(memberId)
        .thenAccept(memberExists -> removeMemberPermission(memberId,
          body.getString("permission"), hasPermission, memberExists)));
  }

  private void removeMemberPermission(
    UUID memberId, String permission, boolean hasPermission, boolean memberExists
  ) {
    if (!hasPermission || !memberExists) {
      return;
    }
    teamDatabaseTable().removeTeamMemberPermission(memberId, permission);
  }

  @RequestMapping(path = "/member/has/permission/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> hasTeamMemberPermission(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    memberHasPermission(findMemberId(request), body.getString("permission"))
      .thenAccept(hasPermission -> futureResponse.complete(
        Map.of("hasPermission", hasPermission)));
    return futureResponse;
  }

  @RequestMapping(path = "/members/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findTeamMembers(
    HttpServletRequest request
  ) {
    return memberHasPermission(findMemberId(request), "members.find")
      .thenCompose(this::findTeamMembers);
  }

  private CompletableFuture<Map<String, Object>> findTeamMembers(
    boolean hasPermission
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return teamDatabaseTable().findAllTeamMembers()
      .thenApply(teamMembers -> Map.of("members",
        findTeamMembersInformation(teamMembers)));
  }

  private List<Map<String, Object>> findTeamMembersInformation(
    List<TeamMember> members
  ) {
    var result = Lists.<Map<String, Object>>newArrayList();
    for (var member : members) {
      result.add(assemblyTeamMemberInformation(member));
    }
    return result;
  }

  private Map<String, Object> assemblyTeamMemberInformation(TeamMember teamMember) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", teamMember.id());
    information.put("name", teamMember.name());
    information.put("group", teamMember.group());
    information.put("permissions", teamMember.permissions());
    return information;
  }
}
