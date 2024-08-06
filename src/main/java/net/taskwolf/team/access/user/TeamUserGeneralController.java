package net.taskwolf.team.access.user;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.iterator.AsyncIterator;
import net.taskwolf.core.organization.Organization;
import net.taskwolf.core.organization.OrganizationDatabaseTable;
import net.taskwolf.core.user.User;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
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
public final class TeamUserGeneralController extends TeamController {
  private final UserDatabaseTable userDatabaseTable;
  private final OrganizationDatabaseTable organizationDatabaseTable;

  private TeamUserGeneralController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, UserDatabaseTable userDatabaseTable,
    OrganizationDatabaseTable organizationDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.userDatabaseTable = userDatabaseTable;
    this.organizationDatabaseTable = organizationDatabaseTable;
  }

  @RequestMapping(path = "/user/search/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> searchUser(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var userMail = body.getString("user");
    return memberHasPermission(findMemberId(request), "user.search")
      .thenCompose(hasPermission -> userDatabaseTable.userExists(userMail)
        .thenCompose(exists -> searchUser(userMail, hasPermission, exists)));
  }

  private CompletableFuture<Map<String, Object>> searchUser(
    String userMail, boolean hasPermission, boolean userExists
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    if (!userExists) {
      return CompletableFuture.completedFuture(Map.of("userFound", false));
    }
    return userDatabaseTable.findUser(userMail)
      .thenApply(user -> Map.of("userFound", true, "userId", user.id()));
  }

  @RequestMapping(path = "/user/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findUser(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var userId = body.getUUID("user");
    return memberHasPermission(findMemberId(request), "user.find")
      .thenCompose(hasPermission -> userDatabaseTable.userExists(userId)
        .thenCompose(exists -> findUser(userId, hasPermission, exists)));
  }

  private CompletableFuture<Map<String, Object>> findUser(
    UUID userId, boolean hasPermission, boolean userExists
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    if (!userExists) {
      return CompletableFuture.completedFuture(Map.of("found", false));
    }
    return userDatabaseTable.findUser(userId)
      .thenCompose(user -> AsyncIterator.execute(user.organizations(),
          organizationDatabaseTable::findOrganization)
        .thenApply(organizations -> assemblyUserInformation(user, organizations)));
  }

  private Map<String, Object> assemblyUserInformation(
    User user, List<Organization> organizations
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("found", true);
    information.put("id", user.id());
    information.put("email", user.email());
    information.put("name", user.name());
    information.put("language", user.language());
    var organizationInformation = Lists.<Map<String, Object>>newArrayList();
    for (var organization : organizations) {
      organizationInformation.add(Map.of("id", organization.id(),
        "name", organization.name()));
    }
    information.put("organizations", organizationInformation);
    return information;
  }
}