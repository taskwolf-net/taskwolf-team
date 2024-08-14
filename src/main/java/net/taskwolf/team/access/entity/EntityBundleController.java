package net.taskwolf.team.access.entity;

import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.bundle.*;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class EntityBundleController extends TeamController {
  private final BundleDatabaseTable bundleDatabaseTable;
  private final BundlePresetRepository bundlePresetRepository;

  private EntityBundleController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    BundleDatabaseTable bundleDatabaseTable,
    BundlePresetRepository bundlePresetRepository
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.bundleDatabaseTable = bundleDatabaseTable;
    this.bundlePresetRepository = bundlePresetRepository;
  }

  @RequestMapping(path = "/entity/bundle/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findEntityBundle(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var entityId = body.getUUID("entity");
    return memberHasPermission(findMemberId(request), "entity.bundle.find")
      .thenCompose(hasPermission -> bundleDatabaseTable.bundleExists(entityId)
        .thenCompose(exists -> findEntityBundle(entityId, hasPermission, exists)));
  }

  private CompletableFuture<Map<String, Object>> findEntityBundle(
    UUID entityId, boolean hasPermission, boolean bundleExists
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    if (!bundleExists) {
      return CompletableFuture.completedFuture(Map.of("found", false));
    }
    return bundleDatabaseTable.findBundle(entityId)
      .thenApply(this::assemblyBundleInformation);
  }

  private Map<String, Object> assemblyBundleInformation(Bundle bundle) {
    var information = Maps.<String, Object>newHashMap();
    information.put("found", true);
    information.put("type", bundle.bundleType().toString());
    information.put("class", bundle.bundleClass().toString());
    information.put("runtime", bundle.bundleRuntime().toString());
    information.put("price", bundle.price());
    information.put("expiration", bundle.expiration());
    information.put("workflowAccess", bundle.workflowAccess());
    information.put("workflowNumberLimit", bundle.workflowNumberLimit());
    information.put("workflowOperationLimit", bundle.workflowOperationLimit());
    information.put("workflowTemplateAccess", bundle.workflowTemplateAccess());
    information.put("processAccess", bundle.processAccess());
    information.put("processNumberLimit", bundle.processNumberLimit());
    information.put("databaseAccess", bundle.databaseAccess());
    information.put("databaseNumberLimit", bundle.databaseNumberLimit());
    information.put("databaseDataLimit", new DecimalFormat("#.#").format(
      bundle.databaseDataLimit()));
    information.put("webhookAccess", bundle.webhookAccess());
    information.put("webhookNumberLimit", bundle.webhookNumberLimit());
    information.put("organizationAccess", bundle.organizationAccess());
    information.put("organizationMemberLimit", bundle.organizationMemberLimit());
    information.put("organizationTeamLimit", bundle.organizationTeamLimit());
    information.put("deviceAccess", bundle.deviceAccess());
    information.put("accountsAccess", bundle.accountsAccess());
    information.put("accountsNumberLimit", bundle.accountsNumberLimit());
    return information;
  }

  @RequestMapping(path = "/entity/bundle/change/", method = RequestMethod.POST)
  public void changeEntityBundle(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var entityId = body.getUUID("entity");
    memberHasPermission(findMemberId(request), "entity.bundle.change")
      .thenAccept(hasPermission -> changeEntityBundle(entityId, body,
        hasPermission));
  }

  private void changeEntityBundle(
    UUID entityId, TaskwolfRequestBody body, boolean hasPermission
  ) {
    if (!hasPermission) {
      return;
    }
    var preset = findBundlePreset(body);
    var runtime = BundleRuntime.valueOf(body.getString("runtime"));
    var price = findBundlePrice(body, preset, runtime);
    var workflowNumberLimit = findBundleWorkflowNumberLimit(body, preset);
    var workflowOperationLimit = findBundleWorkflowOperationLimit(body, preset);
    var processNumberLimit = findBundleProcessNumberLimit(body, preset);
    var databaseNumberLimit = findBundleDatabaseNumberLimit(body, preset);
    var databaseDataLimit = findBundleDatabaseDataLimit(body, preset);
    var isOrganization = preset.bundleType().isTeam() ||
      preset.bundleType().isEnterprise();
    var organizationMemberLimit = isOrganization ?
      findBundleOrganizationMemberLimit(body, preset) : 0;
    var organizationTeamLimit = isOrganization ?
      findBundleOrganizationTeamLimit(body, preset) : 0;
    System.out.println(organizationMemberLimit + ":" + organizationTeamLimit);
    bundleDatabaseTable.deleteBundle(entityId).thenAccept(value ->
      bundleDatabaseTable.insertBundle(Bundle.of(entityId,
        preset, runtime, price, workflowNumberLimit, workflowOperationLimit,
        processNumberLimit, databaseNumberLimit, databaseDataLimit,
        organizationMemberLimit, organizationTeamLimit)));
  }

  private double findBundlePrice(
    TaskwolfRequestBody body, BundlePreset preset, BundleRuntime runtime
  ) {
    if (runtime.isWeekly()) {
      return 0;
    }
    if (!body.has("price")) {
      return preset.hasPrice() ? (runtime.isMonthly() ?
        preset.monthlyPrice() : preset.yearlyPrice()) : 0;
    }
    return body.getDouble("price");
  }

  private long findBundleWorkflowNumberLimit(
    TaskwolfRequestBody body, BundlePreset preset
  ) {
    if (!body.has("workflowNumberLimit")) {
      return preset.workflowNumberLimit();
    }
    return body.getLong("workflowNumberLimit");
  }

  private long findBundleWorkflowOperationLimit(
    TaskwolfRequestBody body, BundlePreset preset
  ) {
    if (!body.has("workflowOperationLimit")) {
      return preset.hasWorkflowOperationLimit() ?
        preset.workflowOperationLimit() : 0;
    }
    return body.getLong("workflowOperationLimit");
  }

  private long findBundleProcessNumberLimit(
    TaskwolfRequestBody body, BundlePreset preset
  ) {
    if (!body.has("processNumberLimit")) {
      return preset.processNumberLimit();
    }
    return body.getLong("processNumberLimit");
  }

  private long findBundleDatabaseNumberLimit(
    TaskwolfRequestBody body, BundlePreset preset
  ) {
    if (!body.has("databaseNumberLimit")) {
      return preset.databaseNumberLimit();
    }
    return body.getLong("databaseNumberLimit");
  }

  private double findBundleDatabaseDataLimit(
    TaskwolfRequestBody body, BundlePreset preset
  ) {
    if (!body.has("databaseDataLimit")) {
      return preset.databaseDataLimit();
    }
    return body.getDouble("databaseDataLimit");
  }

  private long findBundleOrganizationMemberLimit(
    TaskwolfRequestBody body, BundlePreset preset
  ) {
    if (!body.has("organizationMemberLimit")) {
      return preset.organizationMemberLimit();
    }
    return body.getLong("organizationMemberLimit");
  }

  private long findBundleOrganizationTeamLimit(
    TaskwolfRequestBody body, BundlePreset preset
  ) {
    if (!body.has("organizationTeamLimit")) {
      return preset.organizationTeamLimit();
    }
    return body.getLong("organizationTeamLimit");
  }

  private BundlePreset findBundlePreset(TaskwolfRequestBody body) {
    var bundleType = BundleType.valueOf(body.getString("type"));
    if (bundleType.isTrial() || bundleType.isEnterprise()) {
      return bundlePresetRepository.findPreset(bundleType).get();
    }
    var bundleClass = BundleClass.valueOf(body.getString("class"));
    return bundlePresetRepository.findPreset(bundleType, bundleClass).get();
  }

  @RequestMapping(path = "/entity/bundle/delete/", method = RequestMethod.POST)
  public void deleteEntityBundle(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var entityId = body.getUUID("entity");
    memberHasPermission(findMemberId(request), "entity.bundle.delete")
      .thenAccept(hasPermission -> bundleDatabaseTable.bundleExists(entityId)
        .thenAccept(exists -> deleteEntityBundle(entityId, hasPermission, exists)));
  }

  private void deleteEntityBundle(
    UUID entityId, boolean hasPermission, boolean bundleExists
  ) {
    if (!hasPermission || !bundleExists) {
      return;
    }
    bundleDatabaseTable.deleteBundle(entityId);
  }
}