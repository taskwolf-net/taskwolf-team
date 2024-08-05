package net.taskwolf.team.access.user;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.bundle.Bundle;
import net.taskwolf.core.bundle.BundleDatabaseTable;
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
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class TeamUserInformationController extends TeamController {
  private final UserDatabaseTable userDatabaseTable;
  private final BundleDatabaseTable bundleDatabaseTable;
  private final OrganizationDatabaseTable organizationDatabaseTable;

  private TeamUserInformationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, UserDatabaseTable userDatabaseTable,
    BundleDatabaseTable bundleDatabaseTable,
    OrganizationDatabaseTable organizationDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.userDatabaseTable = userDatabaseTable;
    this.bundleDatabaseTable = bundleDatabaseTable;
    this.organizationDatabaseTable = organizationDatabaseTable;
  }

  @RequestMapping(path = "/user/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findUser(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var user = body.getString("user");
    return memberHasPermission(findMemberId(request), "user.find")
      .thenCompose(hasPermission -> userDatabaseTable.userExists(user)
        .thenCompose(exists -> findUser(user, hasPermission, exists)));
  }

  private CompletableFuture<Map<String, Object>> findUser(
    String userMail, boolean hasPermission, boolean userExists
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    if (!userExists) {
      return CompletableFuture.completedFuture(Map.of("userFound", false));
    }
    return userDatabaseTable.findUser(userMail)
      .thenCompose(user -> AsyncIterator.execute(user.organizations(),
        organizationDatabaseTable::findOrganization)
        .thenCompose(organizations -> findBundle(user)
          .thenApply(bundleInformation -> assemblyUserInformation(user,
            organizations, bundleInformation))));
  }

  private Map<String, Object> assemblyUserInformation(
    User user, List<Organization> organizations,
    Map<String, Object> bundleInformation
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("userFound", true);
    information.put("userId", user.id());
    information.put("userEmail", user.email());
    information.put("userName", user.name());
    information.put("language", user.language());
    var organizationInformation = Lists.<Map<String, Object>>newArrayList();
    for (var organization : organizations) {
      organizationInformation.add(Map.of("id", organization.id(),
        "name", organization));
    }
    information.put("organizations", organizationInformation);
    information.putAll(bundleInformation);
    return information;
  }

  private CompletableFuture<Map<String, Object>> findBundle(User user) {
    return bundleDatabaseTable.bundleExists(user.id())
      .thenCompose(exists -> findBundle(user, exists));
  }

  private CompletableFuture<Map<String, Object>> findBundle(
    User user, boolean bundleExists
  ) {
    if (!bundleExists) {
      return CompletableFuture.completedFuture(Map.of("hasBundle", false));
    }
    return bundleDatabaseTable.findBundle(user.id())
      .thenApply(this::assemblyBundleInformation);
  }

  private Map<String, Object> assemblyBundleInformation(Bundle bundle) {
    var information = Maps.<String, Object>newHashMap();
    information.put("hasBundle", true);
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
