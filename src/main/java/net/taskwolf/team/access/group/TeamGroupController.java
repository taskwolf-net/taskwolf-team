package net.taskwolf.team.access.group;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.Group;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class TeamGroupController extends TeamController {
  private TeamGroupController(
    Key secretKey, UserDatabaseTable userDatabaseTable,
    TeamDatabaseTable teamDatabaseTable, GroupDatabaseTable groupDatabaseTable
  ) {
    super(secretKey, userDatabaseTable, teamDatabaseTable, groupDatabaseTable);
  }

  @RequestMapping(path = "/team/group/add/", method = RequestMethod.POST)
  public void addGroup(
    HttpServletRequest request, @RequestBody Map<String, Object> input
  ) {
    if (!isAuthenticatedTeamMember(request)) {
      return;
    }
    var group = (String) input.get("name");
    memberHasPermission(findUserId(request), "group.add").thenAccept(
      hasPermission -> addGroup(group, hasPermission));
  }

  private void addGroup(String group, boolean hasPermission) {
    if (!hasPermission) {
      return;
    }
    groupDatabaseTable().insertGroup(Group.create(group, Lists.newArrayList()));
  }

  @RequestMapping(path = "/team/group/remove/", method = RequestMethod.POST)
  public void removeGroup(
    HttpServletRequest request, @RequestBody Map<String, Object> input
  ) {
    if (!isAuthenticatedTeamMember(request)) {
      return;
    }
    var group = (String) input.get("name");
    memberHasPermission(findUserId(request), "group.remove").thenAccept(
      hasPermission -> removeGroup(group, hasPermission));
  }

  private void removeGroup(String group, boolean hasPermission) {
    if (!hasPermission) {
      return;
    }
    groupDatabaseTable().deleteGroup(group);
  }

  @RequestMapping(path = "/team/group/permission/add/", method = RequestMethod.POST)
  public void addGroupPermission(
    HttpServletRequest request, @RequestBody Map<String, Object> input
  ) {
    if (!isAuthenticatedTeamMember(request)) {
      return;
    }
    var group = (String) input.get("group");
    var permission = (String) input.get("permission");
    memberHasPermission(findUserId(request), "group.permission.add")
      .thenAccept(hasPermission -> groupDatabaseTable().groupExists(group)
        .thenAccept(groupExists -> addGroupPermission(group, permission,
          hasPermission, groupExists)));
  }

  private void addGroupPermission(
    String group, String permission, boolean hasPermission, boolean groupExists
  ) {
    if (!hasPermission || !groupExists) {
      return;
    }
    groupDatabaseTable().addGroupPermission(group, permission);
  }

  @RequestMapping(path = "/team/group/permission/remove/", method = RequestMethod.POST)
  public void removeGroupPermission(
    HttpServletRequest request, @RequestBody Map<String, Object> input
  ) {
    if (!isAuthenticatedTeamMember(request)) {
      return;
    }
    var group = (String) input.get("group");
    var permission = (String) input.get("permission");
    memberHasPermission(findUserId(request), "group.permission.remove")
      .thenAccept(hasPermission -> groupDatabaseTable().groupExists(group)
        .thenAccept(groupExists -> removeGroupPermission(group, permission,
          hasPermission, groupExists)));
  }

  private void removeGroupPermission(
    String group, String permission, boolean hasPermission, boolean groupExists
  ) {
    if (!hasPermission || !groupExists) {
      return;
    }
    groupDatabaseTable().removeGroupPermission(group, permission);
  }

  @RequestMapping(path = "/team/groups/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findGroups(
    HttpServletRequest request
  ) {
    if (!isAuthenticatedTeamMember(request)) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    groupDatabaseTable().findAllGroups().thenAccept(groups ->
      futureResponse.complete(Map.of("groups", groups.stream()
        .map(this::assemblyGroupInformation).toList())));
    return futureResponse;
  }

  private Map<String, Object> assemblyGroupInformation(Group group) {
    var information = Maps.<String, Object>newHashMap();
    information.put("name", group. name());
    information.put("permissions", group.permissions());
    return information;
  }
}
