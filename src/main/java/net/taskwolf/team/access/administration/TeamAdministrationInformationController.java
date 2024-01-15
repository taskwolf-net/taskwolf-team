package net.taskwolf.team.access.administration;

import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.Group;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import net.taskwolf.team.member.TeamMember;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class TeamAdministrationInformationController extends TeamController {
  private final TeamDatabaseTable teamDatabaseTable;
  private final GroupDatabaseTable groupDatabaseTable;

  private TeamAdministrationInformationController(
    Key secretKey, UserDatabaseTable userDatabaseTable,
    TeamDatabaseTable teamDatabaseTable, GroupDatabaseTable groupDatabaseTable
  ) {
    super(secretKey, userDatabaseTable);
    this.teamDatabaseTable = teamDatabaseTable;
    this.groupDatabaseTable = groupDatabaseTable;
  }

  @RequestMapping(path = "/team/members/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findTeamMembers(
    HttpServletRequest request
  ) {
    if (!isAuthenticatedTeamMember(request)) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    teamDatabaseTable.findAllTeamMembers().thenAccept(teamMembers ->
      futureResponse.complete(Map.of("members", teamMembers.stream()
        .map(this::assemblyTeamMemberInformation).toList())));
    return futureResponse;
  }

  private Map<String, Object> assemblyTeamMemberInformation(TeamMember teamMember) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", teamMember.userId());
    information.put("group", teamMember.group());
    information.put("permissions", teamMember.permissions());
    return information;
  }

  @RequestMapping(path = "/team/groups/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findGroups(
    HttpServletRequest request
  ) {
    if (!isAuthenticatedTeamMember(request)) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    groupDatabaseTable.findAllGroups().thenAccept(groups ->
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
