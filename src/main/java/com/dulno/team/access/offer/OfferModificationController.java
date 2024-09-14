package com.dulno.team.access.offer;

import com.google.common.collect.Maps;
import com.stripe.StripeClient;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.PriceUpdateParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.bundle.*;
import com.dulno.core.locale.Translation;
import com.dulno.core.mail.Mail;
import com.dulno.core.offer.Offer;
import com.dulno.core.offer.OfferDatabaseTable;
import com.dulno.core.offer.OfferStatus;
import com.dulno.core.organization.Organization;
import com.dulno.core.organization.OrganizationDatabaseTable;
import com.dulno.core.user.User;
import com.dulno.core.user.UserDatabaseTable;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.dulno.team.offer.TeamOfferConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
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
  private final UserDatabaseTable userDatabaseTable;
  private final OrganizationDatabaseTable organizationDatabaseTable;
  private final Mail saleMail;
  private final Translation translation;

  private OfferModificationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    OfferDatabaseTable offerDatabaseTable,
    BundlePresetRepository bundlePresetRepository,
    TeamOfferConfiguration teamOfferConfiguration, StripeClient stripeClient,
    UserDatabaseTable userDatabaseTable,
    OrganizationDatabaseTable organizationDatabaseTable,
    @Qualifier("saleMail") Mail saleMail, Translation translation
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.offerDatabaseTable = offerDatabaseTable;
    this.bundlePresetRepository = bundlePresetRepository;
    this.teamOfferConfiguration = teamOfferConfiguration;
    this.stripeClient = stripeClient;
    this.userDatabaseTable = userDatabaseTable;
    this.organizationDatabaseTable = organizationDatabaseTable;
    this.saleMail = saleMail;
    this.translation = translation;
  }

  @RequestMapping(path = "/offer/price/suggestion/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> createOfferPriceSuggestion(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    return memberHasPermission(findMemberId(request), "offer.price.suggestion")
      .thenApply(hasPermission -> createOfferPriceSuggestion(body, hasPermission));
  }

  private Map<String, Object> createOfferPriceSuggestion(
    DulnoRequestBody body, boolean hasPermission
  ) {
    if (!hasPermission) {
      return Maps.newHashMap();
    }
    var preset = findBundlePreset(body);
    var runtime = BundleRuntime.valueOf(body.getString("runtime"));
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
    var price = teamOfferConfiguration.workflowPrice() * workflowNumberLimit +
      teamOfferConfiguration.workflowOperationPrice() * workflowOperationLimit +
      teamOfferConfiguration.processPrice() * processNumberLimit +
      teamOfferConfiguration.databasePrice() * databaseNumberLimit +
      teamOfferConfiguration.databaseDataPrice() * databaseDataLimit +
      teamOfferConfiguration.organizationMemberPrice() * organizationMemberLimit +
      teamOfferConfiguration.organizationTeamPrice() * organizationTeamLimit;
    if (runtime.isMonthly()) {
      price *= 4D / 3;
    }
    return Map.of("price", Math.round(price * 100.0) / 100.0);
  }

  @RequestMapping(path = "/offer/create/", method = RequestMethod.POST)
  public void createOffer(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var targetId = body.getUUID("target");
    memberHasPermission(findMemberId(request), "offer.create")
      .thenAcceptAsync(hasPermission -> createOffer(targetId, body, hasPermission));
  }

  private void createOffer(
    UUID targetId, DulnoRequestBody body, boolean hasPermission
  ) {
    if (!hasPermission) {
      return;
    }
    var preset = findBundlePreset(body);
    var runtime = BundleRuntime.valueOf(body.getString("runtime"));
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
    var price = Math.round(body.getDouble("price") * 100.0) / 100.0;
    var priceId = createStripeOffer(price);
    offerDatabaseTable.generateAvailableOfferId()
      .thenAccept(id -> createOffer(id, targetId, priceId, preset, runtime,
        price, workflowNumberLimit, workflowOperationLimit, processNumberLimit,
        databaseNumberLimit, databaseDataLimit, organizationMemberLimit,
        organizationTeamLimit));
  }

  private void createOffer(
    UUID id, UUID targetId, String priceId, BundlePreset preset,
    BundleRuntime runtime, double price, long workflowNumberLimit,
    long workflowOperationLimit, long processNumberLimit, long databaseNumberLimit,
    double databaseDataLimit, long organizationMemberLimit, long organizationTeamLimit
  ) {
    offerDatabaseTable.insertOffer(Offer.of(id, targetId,
      priceId, OfferStatus.PENDING, preset, runtime, price, workflowNumberLimit,
      workflowOperationLimit, processNumberLimit, databaseNumberLimit,
      databaseDataLimit, organizationMemberLimit, organizationTeamLimit));
    userDatabaseTable.userExists(targetId)
      .thenCompose(exists -> exists ?
        CompletableFuture.completedFuture(targetId) :
        organizationDatabaseTable.findOrganization(targetId)
          .thenApply(Organization::owner))
      .thenCompose(userDatabaseTable::findUser)
      .thenAccept(user -> sendOfferEmail(user, id));
  }

  private void sendOfferEmail(User user, UUID offerId) {
    var title = translation.translate(user, "offer.email.title");
    var body = String.format(translation.translate(user, "offer.email.body"),
      user.name(), offerId.toString());
    saleMail.send(user.email(), title, body);
  }

  private String createStripeOffer(double price) {
    try {
      var product = PriceCreateParams.ProductData.builder()
        .setName("Manually compiled package")
        .build();
      var recurring = PriceCreateParams.Recurring.builder()
        .setInterval(PriceCreateParams.Recurring.Interval.MONTH)
        .build();
      var parameters = PriceCreateParams.builder()
        .setUnitAmount(Math.round(price * 100))
        .setRecurring(recurring)
        .setCurrency("eur")
        .setProductData(product)
        .build();
      var stripePrice = stripeClient.prices().create(parameters);
      return stripePrice.getId();
    } catch (Exception exception) {
      exception.printStackTrace();
      return null;
    }
  }

  private long findBundleWorkflowNumberLimit(
    DulnoRequestBody body, BundlePreset preset
  ) {
    if (!body.has("workflowNumberLimit")) {
      return preset.workflowNumberLimit();
    }
    return body.getLong("workflowNumberLimit");
  }

  private long findBundleWorkflowOperationLimit(
    DulnoRequestBody body, BundlePreset preset
  ) {
    if (!body.has("workflowOperationLimit")) {
      return preset.hasWorkflowOperationLimit() ?
        preset.workflowOperationLimit() : 0;
    }
    return body.getLong("workflowOperationLimit");
  }

  private long findBundleProcessNumberLimit(
    DulnoRequestBody body, BundlePreset preset
  ) {
    if (!body.has("processNumberLimit")) {
      return preset.processNumberLimit();
    }
    return body.getLong("processNumberLimit");
  }

  private long findBundleDatabaseNumberLimit(
    DulnoRequestBody body, BundlePreset preset
  ) {
    if (!body.has("databaseNumberLimit")) {
      return preset.databaseNumberLimit();
    }
    return body.getLong("databaseNumberLimit");
  }

  private double findBundleDatabaseDataLimit(
    DulnoRequestBody body, BundlePreset preset
  ) {
    if (!body.has("databaseDataLimit")) {
      return preset.databaseDataLimit();
    }
    return body.getDouble("databaseDataLimit");
  }

  private long findBundleOrganizationMemberLimit(
    DulnoRequestBody body, BundlePreset preset
  ) {
    if (!body.has("organizationMemberLimit")) {
      return preset.organizationMemberLimit();
    }
    return body.getLong("organizationMemberLimit");
  }

  private long findBundleOrganizationTeamLimit(
    DulnoRequestBody body, BundlePreset preset
  ) {
    if (!body.has("organizationTeamLimit")) {
      return preset.organizationTeamLimit();
    }
    return body.getLong("organizationTeamLimit");
  }

  private BundlePreset findBundlePreset(DulnoRequestBody body) {
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
    var body = DulnoRequestBody.of(payload, response);
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
    offerDatabaseTable.findOffer(offerId).thenAccept(this::withdrawOffer);
  }

  private void withdrawOffer(Offer offer) {
    offerDatabaseTable.updateOfferStatus(offer, OfferStatus.WITHDRAWN);
    try {
      stripeClient.prices().update(offer.priceId(),
        PriceUpdateParams.builder().setActive(false).build());
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }
}
