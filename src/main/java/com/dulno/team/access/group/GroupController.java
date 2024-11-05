package com.dulno.team.access.group;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.Group;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class GroupController extends TeamController {
  private GroupController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
  }

  @RequestMapping(path = "/group/add/", method = RequestMethod.POST)
  public void addGroup(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var priority = Integer.parseInt(body.getString("priority"));
    memberHasPermission(findMemberId(request), "group.add").thenAccept(
      hasPermission -> addGroup(body.getString("name"), priority,
        body.getString("color"), hasPermission));
  }

  private void addGroup(
    String group, int priority, String color, boolean hasPermission
  ) {
    if (!hasPermission) {
      return;
    }
    groupDatabaseTable().insertGroup(Group.create(group, priority, color,
      Lists.newArrayList()));
  }

  @RequestMapping(path = "/group/remove/", method = RequestMethod.POST)
  public void removeGroup(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    memberHasPermission(findMemberId(request), "group.remove").thenAccept(
      hasPermission -> removeGroup(body.getString("name"), hasPermission));
  }

  private void removeGroup(String group, boolean hasPermission) {
    if (!hasPermission) {
      return;
    }
    groupDatabaseTable().deleteGroup(group);
  }

  @RequestMapping(path = "/group/rename/", method = RequestMethod.POST)
  public void renameGroup(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    memberHasPermission(findMemberId(request), "group.rename").thenAccept(
      hasPermission -> renameGroup(body.getString("currentName"),
        body.getString("newName"), hasPermission));
  }

  private void renameGroup(String group, String newName, boolean hasPermission) {
    if (!hasPermission) {
      return;
    }
    groupDatabaseTable().renameGroup(group, newName);
    teamDatabaseTable().findTeamMembersOfGroup(group)
      .thenAccept(members -> members.forEach(member ->
        teamDatabaseTable().changeTeamMemberGroup(member, newName)));
  }

  @RequestMapping(path = "/group/priority/change/", method = RequestMethod.POST)
  public void changeGroupPriority(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var group = body.getString("name");
    var priority = Integer.parseInt(body.getString("priority"));
    memberHasPermission(findMemberId(request), "group.priority.change")
      .thenAccept(hasPermission -> groupDatabaseTable().groupExists(group)
        .thenAccept(groupExists -> changeGroupPriority(group, priority,
          hasPermission, groupExists)));
  }

  private void changeGroupPriority(
    String group, int priority, boolean hasPermission, boolean groupExists
  ) {
    if (!hasPermission || !groupExists) {
      return;
    }
    groupDatabaseTable().changeGroupPriority(group, priority);
  }

  @RequestMapping(path = "/group/color/change/", method = RequestMethod.POST)
  public void changeGroupColor(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var group = body.getString("name");
    memberHasPermission(findMemberId(request), "group.color.change")
      .thenAccept(hasPermission -> groupDatabaseTable().groupExists(group)
        .thenAccept(groupExists -> changeGroupColor(group,
          body.getString("color"), hasPermission, groupExists)));
  }

  private void changeGroupColor(
    String group, String color, boolean hasPermission, boolean groupExists
  ) {
    if (!hasPermission || !groupExists) {
      return;
    }
    groupDatabaseTable().changeGroupColor(group, color);
  }

  @RequestMapping(path = "/group/permission/add/", method = RequestMethod.POST)
  public void addGroupPermission(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var group = body.getString("name");
    memberHasPermission(findMemberId(request), "group.permission.add")
      .thenAccept(hasPermission -> groupDatabaseTable().groupExists(group)
        .thenAccept(groupExists -> addGroupPermission(group,
          body.getString("permission"), hasPermission, groupExists)));
  }

  private void addGroupPermission(
    String group, String permission, boolean hasPermission, boolean groupExists
  ) {
    if (!hasPermission || !groupExists) {
      return;
    }
    groupDatabaseTable().addGroupPermission(group, permission);
  }

  @RequestMapping(path = "/group/permission/remove/", method = RequestMethod.POST)
  public void removeGroupPermission(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var group = body.getString("name");
    memberHasPermission(findMemberId(request), "group.permission.remove")
      .thenAccept(hasPermission -> groupDatabaseTable().groupExists(group)
        .thenAccept(groupExists -> removeGroupPermission(group,
          body.getString("permission"), hasPermission, groupExists)));
  }

  private void removeGroupPermission(
    String group, String permission, boolean hasPermission, boolean groupExists
  ) {
    if (!hasPermission || !groupExists) {
      return;
    }
    groupDatabaseTable().removeGroupPermission(group, permission);
  }

  @RequestMapping(path = "/groups/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findGroups(
    HttpServletRequest request
  ) {
    return memberHasPermission(findMemberId(request), "groups.find")
      .thenCompose(this::findGroups);
  }

  private CompletableFuture<Map<String, Object>> findGroups(boolean hasPermission) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return groupDatabaseTable().findAllGroups()
      .thenApply(groups -> Map.of("groups", groups.stream()
        .map(this::assemblyGroupInformation).toList()));
  }

  @RequestMapping(path = "/group/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findMemberGroup(
    HttpServletRequest request
  ) {
    return findMember(request)
      .thenCompose(member -> groupDatabaseTable().findGroup(member.group()))
      .thenApply(this::assemblyGroupInformation);
  }

  private Map<String, Object> assemblyGroupInformation(Group group) {
    var information = Maps.<String, Object>newHashMap();
    information.put("name", group.name());
    information.put("priority", group.priority());
    information.put("color", group.color());
    information.put("permissions", group.permissions());
    return information;
  }
}
