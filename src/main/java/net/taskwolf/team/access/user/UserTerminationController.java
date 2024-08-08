package net.taskwolf.team.access.user;

import com.google.common.collect.Maps;
import com.stripe.StripeClient;
import com.stripe.param.SubscriptionListParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.bundle.Bundle;
import net.taskwolf.core.bundle.BundleDatabaseTable;
import net.taskwolf.core.stripe.StripeAccount;
import net.taskwolf.core.stripe.StripeDatabaseTable;
import net.taskwolf.core.stripe.TerminationDatabaseTable;
import net.taskwolf.team.access.TeamController;
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
public final class UserTerminationController extends TeamController {
  private final BundleDatabaseTable bundleDatabaseTable;
  private final StripeDatabaseTable stripeDatabaseTable;
  private final TerminationDatabaseTable terminationDatabaseTable;
  private final StripeClient stripeClient;

  private UserTerminationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    BundleDatabaseTable bundleDatabaseTable,
    StripeDatabaseTable stripeDatabaseTable,
    TerminationDatabaseTable terminationDatabaseTable,
    StripeClient stripeClient
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.bundleDatabaseTable = bundleDatabaseTable;
    this.stripeDatabaseTable = stripeDatabaseTable;
    this.terminationDatabaseTable = terminationDatabaseTable;
    this.stripeClient = stripeClient;
  }

  @RequestMapping(path = "/user/termination/status/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findUserTerminationStatus(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var userId = body.getUUID("user");
    return memberHasPermission(findMemberId(request), "user.termination.status")
      .thenCompose(hasPermission -> bundleDatabaseTable.bundleExists(userId)
        .thenCompose(bundleExists ->
          stripeDatabaseTable.stripeAccountExistsByTarget(userId)
            .thenCompose(stripeExists -> findUserTerminationStatus(userId,
              hasPermission, bundleExists, stripeExists))));
  }

  private CompletableFuture<Map<String, Object>> findUserTerminationStatus(
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
      .thenCompose(bundle -> stripeDatabaseTable.findStripeAccountByTarget(targetId)
        .thenCompose(stripeAccount -> findUserTerminationStatus(targetId, bundle,
          stripeAccount.accountId())));
  }

  private CompletableFuture<Map<String, Object>> findUserTerminationStatus(
    UUID targetId, Bundle bundle, String stripeAccountId
  ) {
    try {
      var subscriptions = stripeClient.subscriptions()
        .list(SubscriptionListParams.builder().setCustomer(stripeAccountId).build())
        .getData();
      if (subscriptions.isEmpty()) {
        return CompletableFuture.completedFuture(Map.of("terminable", true,
          "terminated", true));
      }
      return terminationDatabaseTable.terminationExists(targetId)
        .thenApply(exists -> findUserTerminationStatus(bundle, exists));
    } catch (Exception exception) {
      exception.printStackTrace();
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
  }

  private Map<String, Object> findUserTerminationStatus(
    Bundle bundle, boolean terminationExists
  ) {
    if (!terminationExists) {
      return Map.of("terminable", true, "terminated", false, "directlyEffective",
        bundle.expiration() - System.currentTimeMillis() < 1000L * 60 * 60 * 24 * 30);
    }
    return Map.of("terminable", true, "terminated", true);
  }

  @RequestMapping(path = "/user/bundle/terminate/", method = RequestMethod.POST)
  public void terminateUserBundle(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var userId = body.getUUID("user");
    memberHasPermission(findMemberId(request), "user.bundle.terminate")
      .thenAccept(hasPermission -> bundleDatabaseTable.bundleExists(userId)
        .thenAccept(bundleExists ->
          stripeDatabaseTable.stripeAccountExistsByTarget(userId)
            .thenAccept(stripeExists -> terminateUserBundle(userId,
              hasPermission, bundleExists, stripeExists))));
  }

  private void terminateUserBundle(
    UUID targetId, boolean hasPermission, boolean bundleExists,
    boolean stripeAccountExists
  ) {
    if (!hasPermission || !bundleExists || !stripeAccountExists) {
      return;
    }
    bundleDatabaseTable.findBundle(targetId)
      .thenAccept(bundle -> stripeDatabaseTable.findStripeAccountByTarget(targetId)
        .thenAccept(stripeAccount -> terminateUserBundle(targetId, bundle,
          stripeAccount)));
  }

  private void terminateUserBundle(
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
      var subscriptions = stripeClient.subscriptions()
        .list(SubscriptionListParams.builder().setCustomer(account.accountId()).build())
        .getData();
      if (subscriptions.isEmpty()) {
        return;
      }
      subscriptions.get(0).cancel();
      stripeDatabaseTable.updateStripeAccount(account.accountId(),
        account.targetId(), account.userId(), "");
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }
}