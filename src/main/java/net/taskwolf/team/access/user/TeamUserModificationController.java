package net.taskwolf.team.access.user;

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
import java.util.UUID;

@RestController
public final class TeamUserModificationController extends TeamController {
  private final UserDatabaseTable userDatabaseTable;
  private final BundleDatabaseTable bundleDatabaseTable;
  private final BundlePresetRepository bundlePresetRepository;

  private TeamUserModificationController(
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