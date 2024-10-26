package com.dulno.team.access.entity;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.iterator.AsyncIterator;
import com.dulno.core.locale.Translation;
import com.dulno.core.organization.Organization;
import com.dulno.core.organization.OrganizationDatabaseTable;
import com.dulno.core.organization.team.Team;
import com.dulno.core.user.User;
import com.dulno.core.user.UserDatabaseTable;
import com.dulno.core.user.UserTargetDatabaseTable;
import com.dulno.team.access.TeamController;
import com.dulno.team.distribution.TeamDistributionConfiguration;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@RestController
public final class OrganizationController extends TeamController {
  private final OrganizationDatabaseTable organizationDatabaseTable;
  private final com.dulno.core.organization.team.TeamDatabaseTable organizationTeamDatabaseTable;
  private final UserDatabaseTable userDatabaseTable;
  private final Translation translation;
  private final TeamDistributionConfiguration teamDistributionConfiguration;
  private final UserTargetDatabaseTable targetDatabaseTable;
  private final HttpClient httpClient = HttpClient.newHttpClient();

  private OrganizationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    OrganizationDatabaseTable organizationDatabaseTable,
    com.dulno.core.organization.team.TeamDatabaseTable organizationTeamDatabaseTable,
    UserDatabaseTable userDatabaseTable, Translation translation,
    TeamDistributionConfiguration teamDistributionConfiguration,
    UserTargetDatabaseTable targetDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.organizationDatabaseTable = organizationDatabaseTable;
    this.organizationTeamDatabaseTable = organizationTeamDatabaseTable;
    this.userDatabaseTable = userDatabaseTable;
    this.translation = translation;
    this.teamDistributionConfiguration = teamDistributionConfiguration;
    this.targetDatabaseTable = targetDatabaseTable;
  }

  @RequestMapping(path = "/organization/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findOrganization(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var organizationId = body.getUUID("organization");
    return memberHasPermission(findMemberId(request), "organization.find")
      .thenCompose(hasPermission ->
        organizationDatabaseTable.organizationExists(organizationId)
          .thenCompose(exists -> findOrganization(organizationId,
            hasPermission, exists)));
  }

  private CompletableFuture<Map<String, Object>> findOrganization(
    UUID organizationId, boolean hasPermission, boolean organizationExists
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    if (!organizationExists) {
      return CompletableFuture.completedFuture(Map.of("found", false));
    }
    return organizationDatabaseTable.findOrganization(organizationId)
      .thenCompose(organization ->
        AsyncIterator.execute(Stream.concat(organization.members().stream(),
            Stream.of(organization.owner())).toList(), userDatabaseTable::findUser)
          .thenCompose(members ->
            organizationTeamDatabaseTable.findTeamsByOrganization(organizationId)
              .thenApply(teams -> assemblyOrganizationInformation(organization,
                members, teams))));
  }

  private Map<String, Object> assemblyOrganizationInformation(
    Organization organization, List<User> members, List<Team> teams
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("found", true);
    information.put("id", organization.id());
    information.put("name", organization.name());
    information.put("owner", members.stream().filter(member ->
      organization.owner().equals(member.id())).findFirst().get().name());
    var memberInformation = Lists.<Map<String, Object>>newArrayList();
    for (var member : members) {
      memberInformation.add(Map.of("id", member.id(), "name", member.name(),
        "role", organization.owner().equals(member.id()) ? "OWNER" : "MEMBER"));
    }
    information.put("members", memberInformation);
    var teamInformation = Lists.<Map<String, Object>>newArrayList();
    for (var team : teams) {
      teamInformation.add(Map.of("id", team.id(), "name", team.name(),
        "members", team.members().size()));
    }
    information.put("teams", teamInformation);
    return information;
  }

  @RequestMapping(path = "/organization/create/", method = RequestMethod.POST)
  public void createOrganization(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var ownerId = body.getUUID("owner");
    memberHasPermission(findMemberId(request), "organization.create")
      .thenAccept(hasPermission ->
        organizationDatabaseTable.organizationExistsByOwner(ownerId)
          .thenAccept(exists -> createOrganization(ownerId,
            body.getString("name"), hasPermission, exists)));
  }

  private void createOrganization(
    UUID ownerId, String name, boolean hasPermission, boolean organizationExists
  ) {
    if (!hasPermission || organizationExists) {
      return;
    }
    organizationDatabaseTable.generateAvailableOrganizationId()
      .thenAccept(id -> userDatabaseTable.findUser(ownerId)
        .thenAccept(owner -> createOrganization(id, owner, name)));
  }

  private void createOrganization(UUID organizationId, User owner, String name) {
    if (name.isEmpty()) {
      name = String.format(translation.translate(owner,
        "organization.default.name"), owner.name());
    }
    organizationDatabaseTable.insertOrganization(organizationId, name, owner.id(),
      Lists.newArrayList(), UUID.randomUUID().toString());
    userDatabaseTable.addUserOrganization(owner.id(), organizationId);
    sendDistributionRequest(organizationId);
    targetDatabaseTable.changeTarget(owner.id(), organizationId);
  }

  private void sendDistributionRequest(UUID id) {
    var body = new JSONObject(Map.of("key",
      teamDistributionConfiguration.distributionKey(), "user", id)).toString();
    var requestBuilder = HttpRequest.newBuilder().uri(
      URI.create("http://10.96.0.4/v1/distribution/add/"))
      .POST(HttpRequest.BodyPublishers.ofString(body))
      .setHeader("Content-Type", "application/json");
    try {
      httpClient.send(requestBuilder.build(),
        HttpResponse.BodyHandlers.ofString());
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }
}
