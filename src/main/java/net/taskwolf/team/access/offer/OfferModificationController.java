package net.taskwolf.team.access.offer;

import com.google.common.collect.Maps;
import com.stripe.StripeClient;
import com.stripe.param.PriceCreateParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.bundle.*;
import net.taskwolf.core.offer.OfferDatabaseTable;
import net.taskwolf.core.offer.OfferStatus;
import net.taskwolf.core.stripe.StripeDatabaseTable;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import net.taskwolf.team.offer.TeamOfferConfiguration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class OfferModificationController extends TeamController {
  private final OfferDatabaseTable offerDatabaseTable;
  private final BundlePresetRepository bundlePresetRepository;
  private final TeamOfferConfiguration teamOfferConfiguration;
  private final StripeClient stripeClient;

  private OfferModificationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    OfferDatabaseTable offerDatabaseTable,
    BundlePresetRepository bundlePresetRepository,
    TeamOfferConfiguration teamOfferConfiguration, StripeClient stripeClient
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.offerDatabaseTable = offerDatabaseTable;
    this.bundlePresetRepository = bundlePresetRepository;
    this.teamOfferConfiguration = teamOfferConfiguration;
    this.stripeClient = stripeClient;
  }

  @RequestMapping(path = "/offer/price/suggestion/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> createOfferPriceSuggestion(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    return memberHasPermission(findMemberId(request), "offer.price.suggestion")
      .thenApply(hasPermission -> createOfferPriceSuggestion(body, hasPermission));
  }

  private Map<String, Object> createOfferPriceSuggestion(
    TaskwolfRequestBody body, boolean hasPermission
  ) {
    if (!hasPermission) {
      return Maps.newHashMap();
    }
    var preset = findBundlePreset(body);
    var runtime = BundleRuntime.valueOf(body.getString("runtime"));
    var workflowNumberLimit = findBundleWorkflowNumberLimit(body, preset);
    var workflowOperationLimit = findBundleWorkflowOperationLimit(body, preset);
    var databaseNumberLimit = findBundleDatabaseNumberLimit(body, preset);
    var databaseDataLimit = findBundleDatabaseDataLimit(body, preset);
    var price = teamOfferConfiguration.workflowPrice() * workflowNumberLimit +
      teamOfferConfiguration.workflowOperationPrice() * workflowOperationLimit +
      teamOfferConfiguration.databasePrice() * databaseNumberLimit +
      teamOfferConfiguration.databaseDataPrice() * databaseDataLimit;
    if (runtime.isYearly()) {
      price *= 0.75;
    }
    return Map.of("price", price);
  }

  @RequestMapping(path = "/offer/create/", method = RequestMethod.POST)
  public void createOffer(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var targetId = body.getUUID("target");
    memberHasPermission(findMemberId(request), "offer.create")
      .thenAcceptAsync(hasPermission -> createOffer(targetId, body, hasPermission));
  }

  private void createOffer(
    UUID targetId, TaskwolfRequestBody body, boolean hasPermission
  ) {
    if (!hasPermission) {
      return;
    }
    var preset = findBundlePreset(body);
    var runtime = BundleRuntime.valueOf(body.getString("runtime"));
    var workflowNumberLimit = findBundleWorkflowNumberLimit(body, preset);
    var workflowOperationLimit = findBundleWorkflowOperationLimit(body, preset);
    var databaseNumberLimit = findBundleDatabaseNumberLimit(body, preset);
    var databaseDataLimit = findBundleDatabaseDataLimit(body, preset);
    var price = body.getDouble("price");
    var stripePriceId = createStripeOffer(price);
  }

  private String createStripeOffer(double price) {
    try {
      var parameters = PriceCreateParams.builder()
        .setUnitAmount(Math.round(price * 100))
        .build();
      var stripePrice = stripeClient.prices().create(parameters);
      return stripePrice.getId();
    } catch (Exception exception) {
      exception.printStackTrace();
      return null;
    }
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

  private BundlePreset findBundlePreset(TaskwolfRequestBody body) {
    var bundleType = BundleType.valueOf(body.getString("type"));
    if (bundleType.isTrial() || bundleType.isEnterprise()) {
      return bundlePresetRepository.findPreset(bundleType).get();
    }
    var bundleClass = BundleClass.valueOf(body.getString("class"));
    return bundlePresetRepository.findPreset(bundleType, bundleClass).get();
  }

  @RequestMapping(path = "/offer/withdraw/", method = RequestMethod.POST)
  public void withdrawOffer(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var offerId = body.getUUID("offer");
    memberHasPermission(findMemberId(request), "offer.withdraw")
      .thenAccept(hasPermission -> offerDatabaseTable.offerExists(offerId)
        .thenAccept(exists -> withdrawOffer(offerId, hasPermission, exists)));
  }

  private void withdrawOffer(
    UUID offerId, boolean hasPermission, boolean offerExists
  ) {
    if (!hasPermission || !offerExists) {
      return;
    }
    offerDatabaseTable.findOffer(offerId).thenAccept(offer ->
      offerDatabaseTable.updateOfferStatus(offer, OfferStatus.WITHDRAWN));
  }
}
