package com.dulno.team.access.entity;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.bundle.Bundle;
import com.dulno.core.bundle.BundleDatabaseTable;
import com.dulno.core.error.ErrorRepository;
import com.dulno.core.stripe.StripeAccount;
import com.dulno.core.stripe.StripeDatabaseTable;
import com.dulno.core.stripe.TerminationDatabaseTable;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.google.common.collect.Maps;
import com.stripe.StripeClient;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionListParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class EntityTerminationController extends TeamController {
  private final BundleDatabaseTable bundleDatabaseTable;
  private final TerminationDatabaseTable terminationDatabaseTable;
  private final StripeDatabaseTable stripeDatabaseTable;
  private final StripeClient stripeClient;
  private final ErrorRepository errorRepository;

  private EntityTerminationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    BundleDatabaseTable bundleDatabaseTable,
    TerminationDatabaseTable terminationDatabaseTable,
    StripeDatabaseTable stripeDatabaseTable, StripeClient stripeClient,
    ErrorRepository errorRepository
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.bundleDatabaseTable = bundleDatabaseTable;
    this.terminationDatabaseTable = terminationDatabaseTable;
    this.stripeDatabaseTable = stripeDatabaseTable;
    this.stripeClient = stripeClient;
    this.errorRepository = errorRepository;
  }

  @RequestMapping(path = "/entity/termination/status/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findEntityTerminationStatus(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var entityId = body.getUUID("entity");
    return memberHasPermission(findMemberId(request), "entity.termination.status")
      .thenCompose(hasPermission -> bundleDatabaseTable.bundleExists(entityId)
        .thenCompose(bundleExists ->
          stripeDatabaseTable.stripeAccountExistsByTarget(entityId)
            .thenCompose(stripeExists -> findEntityTerminationStatus(
              entityId, hasPermission, bundleExists, stripeExists))));
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
      .thenCompose(bundle -> stripeDatabaseTable.findStripeAccountByTarget(targetId)
        .thenCompose(stripeAccount -> findEntityTerminationStatus(targetId, bundle,
          stripeAccount.accountId())));
  }

  private CompletableFuture<Map<String, Object>> findEntityTerminationStatus(
    UUID targetId, Bundle bundle, String stripeAccountId
  ) {
    if (bundle.bundleType().isTrial() || bundle.bundleRuntime().isUnbound()) {
      return CompletableFuture.completedFuture(Map.of("terminable", false,
        "terminated", false));
    }
    try {
      var subscriptions = stripeClient.subscriptions()
        .list(SubscriptionListParams.builder().setCustomer(stripeAccountId).build())
        .getData();
      if (subscriptions.isEmpty()) {
        return CompletableFuture.completedFuture(Map.of("terminable", true,
          "terminated", true));
      }
      return terminationDatabaseTable.terminationExists(targetId)
        .thenApply(exists -> findEntityTerminationStatus(bundle, exists));
    } catch (Exception exception) {
      errorRepository.processError(exception);
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
  }

  private Map<String, Object> findEntityTerminationStatus(
    Bundle bundle, boolean terminationExists
  ) {
    if (!terminationExists) {
      return Map.of("terminable", true, "terminated", false, "directlyEffective",
        isDirectlyTerminable(bundle));
    }
    return Map.of("terminable", true, "terminated", true);
  }

  @RequestMapping(path = "/entity/bundle/terminate/", method = RequestMethod.POST)
  public void terminateEntityBundle(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var entityId = body.getUUID("entity");
    memberHasPermission(findMemberId(request), "entity.bundle.terminate")
      .thenAccept(hasPermission -> bundleDatabaseTable.bundleExists(entityId)
        .thenAccept(bundleExists ->
          stripeDatabaseTable.stripeAccountExistsByTarget(entityId)
            .thenAccept(stripeExists -> terminateEntityBundle(entityId,
              hasPermission, bundleExists, stripeExists))));
  }

  private void terminateEntityBundle(
    UUID targetId, boolean hasPermission, boolean bundleExists,
    boolean stripeAccountExists
  ) {
    if (!hasPermission || !bundleExists || !stripeAccountExists) {
      return;
    }
    bundleDatabaseTable.findBundle(targetId)
      .thenAccept(bundle -> stripeDatabaseTable.findStripeAccountByTarget(targetId)
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
    if (isDirectlyTerminable(bundle)) {
      cancelSubscription(account);
      return;
    }
    terminationDatabaseTable.insertTermination(targetId);
  }

  private CompletableFuture<Void> cancelSubscription(StripeAccount account) {
    try {
      var subscriptions = stripeClient.subscriptions()
        .list(SubscriptionListParams.builder().setCustomer(account.accountId()).build())
        .getData();
      if (subscriptions.isEmpty()) {
        return CompletableFuture.completedFuture(null);
      }
      return cancelSubscription(subscriptions.get(0), account);
    } catch (Exception exception) {
      errorRepository.processError(exception);
      return CompletableFuture.completedFuture(null);
    }
  }

  private CompletableFuture<Void> cancelSubscription(
    Subscription subscription, StripeAccount account
  ) {
    try {
      subscription.cancel();
      return stripeDatabaseTable.updateStripeAccount(account.accountId(),
        account.targetId(), account.userId(), "");
    } catch (Exception exception) {
      errorRepository.processError(exception);
      return CompletableFuture.completedFuture(null);
    }
  }

  private boolean isDirectlyTerminable(Bundle bundle) {
    var current = ZonedDateTime.ofInstant(Instant.ofEpochMilli(bundle.expiration()),
      ZoneId.systemDefault());
    var next = current.minusMonths(1);
    if (next.getDayOfMonth() != current.getDayOfMonth()) {
      next = next.withDayOfMonth(next.getMonth().length(
        next.toLocalDate().isLeapYear()));
    }
    return System.currentTimeMillis() > next.toInstant().toEpochMilli();
  }
}