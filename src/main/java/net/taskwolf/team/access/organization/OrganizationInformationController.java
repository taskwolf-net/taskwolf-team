package net.taskwolf.team.access.organization;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.bundle.Bundle;
import net.taskwolf.core.bundle.BundleDatabaseTable;
import net.taskwolf.core.organization.Organization;
import net.taskwolf.core.organization.OrganizationDatabaseTable;
import net.taskwolf.core.organization.team.Team;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class OrganizationInformationController extends TeamController {
  private final OrganizationDatabaseTable organizationDatabaseTable;
  private final net.taskwolf.core.organization.team.TeamDatabaseTable organizationTeamDatabaseTable;
  private final BundleDatabaseTable bundleDatabaseTable;

  private OrganizationInformationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    OrganizationDatabaseTable organizationDatabaseTable,
    net.taskwolf.core.organization.team.TeamDatabaseTable organizationTeamDatabaseTable,
    BundleDatabaseTable bundleDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.organizationDatabaseTable = organizationDatabaseTable;
    this.organizationTeamDatabaseTable = organizationTeamDatabaseTable;
    this.bundleDatabaseTable = bundleDatabaseTable;
  }

  @RequestMapping(path = "/organization/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findOrganization(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
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
      return CompletableFuture.completedFuture(Map.of("organizationFound", false));
    }
    return organizationDatabaseTable.findOrganization(organizationId)
      .thenCompose(organization ->
        organizationTeamDatabaseTable.findTeamsByOrganization(organizationId)
          .thenCompose(teams -> bundleDatabaseTable.findBundle(organization.id())
            .thenApply(this::assemblyBundleInformation)
            .thenApply(bundleInformation -> assemblyOrganizationInformation(
              organization, teams, bundleInformation))));
  }

  private Map<String, Object> assemblyOrganizationInformation(
    Organization organization, List<Team> teams,
    Map<String, Object> bundleInformation
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("organizationFound", true);
    information.put("organizationId", organization.id());
    information.put("organizationName", organization.name());
    var teamInformation = Lists.<Map<String, Object>>newArrayList();
    for (var team : teams) {
      teamInformation.add(Map.of("id", team.id(), "name", team.name()));
    }
    information.put("teams", teamInformation);
    information.putAll(bundleInformation);
    return information;
  }

  private Map<String, Object> assemblyBundleInformation(Bundle bundle) {
    var information = Maps.<String, Object>newHashMap();
    information.put("bundleType", bundle.bundleType().toString());
    information.put("bundleClass", bundle.bundleClass().toString());
    information.put("bundleRuntime", bundle.bundleRuntime().toString());
    information.put("bundlePrice", bundle.price());
    information.put("bundleExpiration", bundle.expiration());
    information.put("bundleWorkflowAccess", bundle.workflowAccess());
    information.put("bundleWorkflowNumberLimit", bundle.workflowNumberLimit());
    information.put("bundleWorkflowOperationLimit", bundle.workflowOperationLimit());
    information.put("bundleWorkflowTemplateAccess", bundle.workflowTemplateAccess());
    information.put("bundleDatabaseAccess", bundle.databaseAccess());
    information.put("bundleDatabaseNumberLimit", bundle.databaseNumberLimit());
    information.put("bundleDatabaseDataLimit", new DecimalFormat("#.#").format(
      bundle.databaseDataLimit()));
    information.put("bundleWebhookAccess", bundle.webhookAccess());
    information.put("bundleWebhookNumberLimit", bundle.webhookNumberLimit());
    information.put("bundleOrganizationAccess", bundle.organizationAccess());
    information.put("bundleOrganizationMemberLimit", bundle.organizationMemberLimit());
    information.put("bundleOrganizationTeamLimit", bundle.organizationTeamLimit());
    information.put("bundleDeviceAccess", bundle.deviceAccess());
    information.put("bundleAccountsAccess", bundle.accountsAccess());
    information.put("bundleAccountsNumberLimit", bundle.accountsNumberLimit());
    return information;
  }
}
