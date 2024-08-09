package net.taskwolf.team.access.entity;

import com.google.common.collect.Maps;
import com.stripe.StripeClient;
import com.stripe.param.SubscriptionListParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.bundle.Bundle;
import net.taskwolf.core.bundle.BundleDatabaseTable;
import net.taskwolf.core.organization.OrganizationDatabaseTable;
import net.taskwolf.core.stripe.StripeAccount;
import net.taskwolf.core.stripe.StripeDatabaseTable;
import net.taskwolf.core.stripe.TerminationDatabaseTable;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.access.stripe.StripeController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class EntityTerminationController extends StripeController {
  private final BundleDatabaseTable bundleDatabaseTable;
  private final TerminationDatabaseTable terminationDatabaseTable;

  private EntityTerminationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    StripeDatabaseTable stripeDatabaseTable,
    StripeClient stripeClient, UserDatabaseTable userDatabaseTable,
    OrganizationDatabaseTable organizationDatabaseTable,
    BundleDatabaseTable bundleDatabaseTable,
    TerminationDatabaseTable terminationDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable,
      stripeDatabaseTable, stripeClient, userDatabaseTable,
      organizationDatabaseTable);
    this.bundleDatabaseTable = bundleDatabaseTable;
    this.terminationDatabaseTable = terminationDatabaseTable;
  }

  @RequestMapping(path = "/entity/termination/status/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findEntityTerminationStatus(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var entityId = body.getUUID("entity");
    return memberHasPermission(findMemberId(request), "entity.termination.status")
      .thenCompose(hasPermission -> findStripeTarget(entityId)
        .thenCompose(target -> bundleDatabaseTable.bundleExists(target)
          .thenCompose(bundleExists ->
            stripeDatabaseTable().stripeAccountExistsByTarget(target)
              .thenCompose(stripeExists -> findEntityTerminationStatus(
                target, hasPermission, bundleExists, stripeExists)))));
  }

  private CompletableFuture<Map<String, Object>> findEntityTerminationStatus(
    UUID targetId, boolean hasPermission, boolean bundleExists,
    boolean stripeAccountExists
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    if (!bundleExists || !stripeAccountExists) {
      return CompletableFuture.completedFuture(Map.of("terminable", false));
    }
    return bundleDatabaseTable.findBundle(targetId)
      .thenCompose(bundle -> stripeDatabaseTable().findStripeAccountByTarget(targetId)
        .thenCompose(stripeAccount -> findEntityTerminationStatus(targetId, bundle,
          stripeAccount.accountId())));
  }

  private CompletableFuture<Map<String, Object>> findEntityTerminationStatus(
    UUID targetId, Bundle bundle, String stripeAccountId
  ) {
    try {
      var subscriptions = stripeClient().subscriptions()
        .list(SubscriptionListParams.builder().setCustomer(stripeAccountId).build())
        .getData();
      if (subscriptions.isEmpty()) {
        return CompletableFuture.completedFuture(Map.of("terminable", true,
          "terminated", true));
      }
      return terminationDatabaseTable.terminationExists(targetId)
        .thenApply(exists -> findEntityTerminationStatus(bundle, exists));
    } catch (Exception exception) {
      exception.printStackTrace();
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
  }

  private Map<String, Object> findEntityTerminationStatus(
    Bundle bundle, boolean terminationExists
  ) {
    if (!terminationExists) {
      return Map.of("terminable", true, "terminated", false, "directlyEffective",
        bundle.expiration() - System.currentTimeMillis() < 1000L * 60 * 60 * 24 * 30);
    }
    return Map.of("terminable", true, "terminated", true);
  }

  @RequestMapping(path = "/entity/bundle/terminate/", method = RequestMethod.POST)
  public void terminateEntityBundle(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var entityId = body.getUUID("entity");
    memberHasPermission(findMemberId(request), "entity.bundle.terminate")
      .thenAccept(hasPermission -> findStripeTarget(entityId)
        .thenCompose(target -> bundleDatabaseTable.bundleExists(target)
          .thenAccept(bundleExists ->
            stripeDatabaseTable().stripeAccountExistsByTarget(target)
              .thenAccept(stripeExists -> terminateEntityBundle(target,
                hasPermission, bundleExists, stripeExists)))));
  }

  private void terminateEntityBundle(
    UUID targetId, boolean hasPermission, boolean bundleExists,
    boolean stripeAccountExists
  ) {
    if (!hasPermission || !bundleExists || !stripeAccountExists) {
      return;
    }
    bundleDatabaseTable.findBundle(targetId)
      .thenAccept(bundle -> stripeDatabaseTable().findStripeAccountByTarget(targetId)
        .thenAccept(stripeAccount -> terminateEntityBundle(targetId, bundle,
          stripeAccount)));
  }

  private void terminateEntityBundle(
    UUID targetId, Bundle bundle, StripeAccount account
  ) {
    if (bundle.bundleRuntime().isMonthly()) {
      cancelSubscription(account);
    } else if (bundle.bundleRuntime().isYearly()) {
      terminateYearly(targetId, bundle, account);
    }
  }

  private void terminateYearly(UUID targetId, Bundle bundle, StripeAccount account) {
    if (bundle.expiration() - System.currentTimeMillis() < 1000L * 60 * 60 * 24 * 30) {
      cancelSubscription(account);
      return;
    }
    terminationDatabaseTable.insertTermination(targetId);
  }

  public void cancelSubscription(StripeAccount account) {
    try {
      var subscriptions = stripeClient().subscriptions()
        .list(SubscriptionListParams.builder().setCustomer(account.accountId()).build())
        .getData();
      if (subscriptions.isEmpty()) {
        return;
      }
      subscriptions.get(0).cancel();
      stripeDatabaseTable().updateStripeAccount(account.accountId(),
        account.targetId(), account.userId(), "");
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }
}