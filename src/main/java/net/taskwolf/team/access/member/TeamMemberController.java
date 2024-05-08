package net.taskwolf.team.access.member;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.iterator.AsyncIterator;
import net.taskwolf.core.user.ProfilePictureDatabaseTable;
import net.taskwolf.core.user.User;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import net.taskwolf.team.member.TeamMember;
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
public final class TeamMemberController extends TeamController {
  private final ProfilePictureDatabaseTable profilePictureDatabaseTable;

  private TeamMemberController(
    Key secretKey, UserDatabaseTable userDatabaseTable,
    TeamDatabaseTable teamDatabaseTable, GroupDatabaseTable groupDatabaseTable,
    ProfilePictureDatabaseTable profilePictureDatabaseTable
  ) {
    super(secretKey, userDatabaseTable, teamDatabaseTable, groupDatabaseTable);
    this.profilePictureDatabaseTable = profilePictureDatabaseTable;
  }

  @RequestMapping(path = "/team/member/add/", method = RequestMethod.POST)
  public void addMember(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    if (!isAuthenticatedTeamMember(request)) {
      return;
    }
    var targetEmail = body.getString("email");
    memberHasPermission(findUserId(request), "member.add").thenAccept(
      hasPermission -> userDatabaseTable().userExists(targetEmail).thenAccept(
        targetExists -> addMember(targetEmail, body.getString("group"),
          hasPermission, targetExists)));
  }

  private void addMember(
    String targetEmail, String targetGroup, boolean hasPermission,
    boolean targetExists
  ) {
    if (!hasPermission || !targetExists) {
      return;
    }
    userDatabaseTable().findUser(targetEmail).thenAccept(user ->
      teamDatabaseTable().insertTeamMember(TeamMember.create(user.id(), targetGroup,
        Lists.newArrayList())));
  }

  @RequestMapping(path = "/team/member/remove/", method = RequestMethod.POST)
  public void removeMember(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    if (!isAuthenticatedTeamMember(request)) {
      return;
    }
    var memberId = body.getUUID("member");
    memberHasPermission(findUserId(request), "member.remove").thenAccept(
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
  }

  @RequestMapping(path = "/team/member/group/change/", method = RequestMethod.POST)
  public void changeMemberGroup(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    if (!isAuthenticatedTeamMember(request)) {
      return;
    }
    var memberId = body.getUUID("member");
    memberHasPermission(findUserId(request), "member.group.change")
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

  @RequestMapping(path = "/team/member/permission/add/", method = RequestMethod.POST)
  public void addMemberPermission(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    if (!isAuthenticatedTeamMember(request)) {
      return;
    }
    var memberId = body.getUUID("member");
    memberHasPermission(findUserId(request), "member.permission.add")
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

  @RequestMapping(path = "/team/member/permission/remove/", method = RequestMethod.POST)
  public void removeMemberPermission(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    if (!isAuthenticatedTeamMember(request)) {
      return;
    }
    var memberId = body.getUUID("member");
    memberHasPermission(findUserId(request), "member.permission.remove")
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

  @RequestMapping(path = "/team/member/has/permission/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> hasTeamMemberPermission(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    if (!isAuthenticatedTeamMember(request)) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    var userId = findUserId(request);
    memberHasPermission(userId, body.getString("permission")).thenAccept(
      hasPermission -> futureResponse.complete(Map.of("hasPermission", hasPermission)));
    return futureResponse;
  }

  @RequestMapping(path = "/team/members/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findTeamMembers(
    HttpServletRequest request
  ) {
    if (!isAuthenticatedTeamMember(request)) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    teamDatabaseTable().findAllTeamMembers().thenAccept(teamMembers ->
      findTeamMembersInformation(teamMembers).thenAccept(information ->
        futureResponse.complete(Map.of("members", information))));
    return futureResponse;
  }

  private CompletableFuture<List<Map<String, Object>>> findTeamMembersInformation(
    List<TeamMember> members
  ) {
    var futureResponse = new CompletableFuture<List<Map<String, Object>>>();
    AsyncIterator.execute(members, this::findTeamMemberInformation)
      .thenAccept(futureResponse::complete);
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> findTeamMemberInformation(
    TeamMember member
  ) {
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    userDatabaseTable().findUser(member.userId()).thenAccept(user ->
      profilePictureDatabaseTable.findProfilePicture(user.id()).thenAccept(picture ->
        futureResponse.complete(assemblyTeamMemberInformation(member, user, picture))));
    return futureResponse;
  }

  private Map<String, Object> assemblyTeamMemberInformation(
    TeamMember teamMember, User user, String profilePicture
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", teamMember.userId());
    information.put("name", user.name());
    information.put("profilePicture", profilePicture);
    information.put("group", teamMember.group());
    information.put("permissions", teamMember.permissions());
    return information;
  }
}
