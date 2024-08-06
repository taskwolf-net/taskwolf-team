package net.taskwolf.team.access.user;

import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.bundle.*;
import net.taskwolf.core.user.UserDatabaseTable;
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
public final class TeamUserBundleController extends TeamController {
  private final UserDatabaseTable userDatabaseTable;
  private final BundleDatabaseTable bundleDatabaseTable;
  private final BundlePresetRepository bundlePresetRepository;

  private TeamUserBundleController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, UserDatabaseTable userDatabaseTable,
    BundleDatabaseTable bundleDatabaseTable,
    BundlePresetRepository bundlePresetRepository
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.userDatabaseTable = userDatabaseTable;
    this.bundleDatabaseTable = bundleDatabaseTable;
    this.bundlePresetRepository = bundlePresetRepository;
  }

  @RequestMapping(path = "/user/bundle/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findUserBundle(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var userId = body.getUUID("user");
    return memberHasPermission(findMemberId(request), "user.bundle.find")
      .thenCompose(hasPermission -> bundleDatabaseTable.bundleExists(userId)
        .thenCompose(exists -> findUserBundle(userId, hasPermission, exists)));
  }

  private CompletableFuture<Map<String, Object>> findUserBundle(
    UUID userId, boolean hasPermission, boolean bundleExists
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    if (!bundleExists) {
      return CompletableFuture.completedFuture(Map.of("found", false));
    }
    return bundleDatabaseTable.findBundle(userId)
      .thenApply(this::assemblyBundleInformation);
  }

  private Map<String, Object> assemblyBundleInformation(Bundle bundle) {
    var information = Maps.<String, Object>newHashMap();
    information.put("found", true);
    information.put("type", bundle.bundleType().toString());
    information.put("class", bundle.bundleClass().toString());
    information.put("Runtime", bundle.bundleRuntime().toString());
    information.put("price", bundle.price());
    information.put("expiration", bundle.expiration());
    information.put("workflowAccess", bundle.workflowAccess());
    information.put("workflowNumberLimit", bundle.workflowNumberLimit());
    information.put("workflowOperationLimit", bundle.workflowOperationLimit());
    information.put("workflowTemplateAccess", bundle.workflowTemplateAccess());
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

  @RequestMapping(path = "/user/bundle/change/", method = RequestMethod.POST)
  public void changeUserBundle(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var userId = body.getUUID("user");
    memberHasPermission(findMemberId(request), "user.bundle.change")
      .thenAccept(hasPermission -> userDatabaseTable.userExists(userId)
        .thenAccept(exists -> changeUserBundle(userId, body, hasPermission, exists)));
  }

  private void changeUserBundle(
    UUID userId, TaskwolfRequestBody body, boolean hasPermission,
    boolean userExists
  ) {
    if (!hasPermission || !userExists) {
      return;
    }
    bundleDatabaseTable.deleteBundle(userId);
    var bundlePreset = findBundlePreset(body);
    var bundleRuntime = BundleRuntime.valueOf(body.getString("runtime"));
    bundleDatabaseTable.insertBundle(Bundle.of(userId,
      bundlePreset, bundleRuntime, body.getDouble("price"),
      body.getLong("workflowOperationLimit"),
      body.getLong("databaseDataLimit"), -1L, -1L));
  }

  private BundlePreset findBundlePreset(TaskwolfRequestBody body) {
    var bundleType = BundleType.valueOf(body.getString("type"));
    if (bundleType.isTrial() || bundleType.isEnterprise()) {
      return bundlePresetRepository.findPreset(bundleType).get();
    }
    var bundleClass = BundleClass.valueOf(body.getString("class"));
    return bundlePresetRepository.findPreset(bundleType, bundleClass).get();
  }

  @RequestMapping(path = "/user/bundle/delete/", method = RequestMethod.POST)
  public void deleteUserBundle(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var userId = body.getUUID("user");
    memberHasPermission(findMemberId(request), "user.bundle.delete")
      .thenAccept(hasPermission -> userDatabaseTable.userExists(userId)
        .thenAccept(exists -> deleteUserBundle(userId, hasPermission, exists)));
  }

  private void deleteUserBundle(
    UUID userId, boolean hasPermission, boolean userExists
  ) {
    if (!hasPermission || !userExists) {
      return;
    }
    bundleDatabaseTable.deleteBundle(userId);
  }
}