package net.taskwolf.team.access.organization;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.bundle.*;
import net.taskwolf.core.organization.OrganizationDatabaseTable;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.UUID;

@RestController
public final class TeamOrganizationModificationController extends TeamController {
  private final OrganizationDatabaseTable organizationDatabaseTable;
  private final BundleDatabaseTable bundleDatabaseTable;
  private final BundlePresetRepository bundlePresetRepository;

  private TeamOrganizationModificationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    OrganizationDatabaseTable organizationDatabaseTable,
    BundleDatabaseTable bundleDatabaseTable,
    BundlePresetRepository bundlePresetRepository
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.organizationDatabaseTable = organizationDatabaseTable;
    this.bundleDatabaseTable = bundleDatabaseTable;
    this.bundlePresetRepository = bundlePresetRepository;
  }

  @RequestMapping(path = "/organization/bundle/change/", method = RequestMethod.POST)
  public void changeOrganizationBundle(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var organizationId = body.getUUID("organization");
    memberHasPermission(findMemberId(request), "organization.bundle.change")
      .thenAccept(hasPermission ->
        organizationDatabaseTable.organizationExists(organizationId)
          .thenAccept(exists -> changeOrganizationBundle(organizationId, body,
            hasPermission, exists)));
  }

  private void changeOrganizationBundle(
    UUID organizationId, TaskwolfRequestBody body, boolean hasPermission,
    boolean organizationExists
  ) {
    if (!hasPermission || !organizationExists) {
      return;
    }
    bundleDatabaseTable.deleteBundle(organizationId);
    var bundlePreset = findBundlePreset(body);
    var bundleRuntime = BundleRuntime.valueOf(body.getString("runtime"));
    bundleDatabaseTable.insertBundle(Bundle.of(organizationId,
      bundlePreset, bundleRuntime, body.getDouble("price"),
      body.getLong("workflowOperationLimit"),
      body.getLong("databaseDataLimit"), body.getLong("organizationMemberLimit"),
      body.getLong("organizationTeamLimit")));
  }

  private BundlePreset findBundlePreset(TaskwolfRequestBody body) {
    var bundleType = BundleType.valueOf(body.getString("type"));
    if (bundleType.isTrial() || bundleType.isEnterprise()) {
      return bundlePresetRepository.findPreset(bundleType).get();
    }
    var bundleClass = BundleClass.valueOf(body.getString("class"));
    return bundlePresetRepository.findPreset(bundleType, bundleClass).get();
  }

  @RequestMapping(path = "/organization/bundle/delete/", method = RequestMethod.POST)
  public void deleteOrganizationBundle(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var organizationId = body.getUUID("organization");
    memberHasPermission(findMemberId(request), "organization.bundle.delete")
      .thenAccept(hasPermission ->
        organizationDatabaseTable.organizationExists(organizationId)
          .thenAccept(exists -> deleteOrganizationBundle(organizationId,
            hasPermission, exists)));
  }

  private void deleteOrganizationBundle(
    UUID organizationId, boolean hasPermission, boolean organizationExists
  ) {
    if (!hasPermission || !organizationExists) {
      return;
    }
    bundleDatabaseTable.deleteBundle(organizationId);
  }
}