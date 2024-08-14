package net.taskwolf.team.access.offer;

import com.google.common.collect.Maps;
import com.stripe.StripeClient;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.PriceUpdateParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.bundle.*;
import net.taskwolf.core.mail.TaskwolfMail;
import net.taskwolf.core.offer.Offer;
import net.taskwolf.core.offer.OfferDatabaseTable;
import net.taskwolf.core.offer.OfferStatus;
import net.taskwolf.core.organization.Organization;
import net.taskwolf.core.organization.OrganizationDatabaseTable;
import net.taskwolf.core.user.User;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import net.taskwolf.team.offer.TeamOfferConfiguration;
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
  private final TaskwolfMail saleMail;

  private OfferModificationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    OfferDatabaseTable offerDatabaseTable,
    BundlePresetRepository bundlePresetRepository,
    TeamOfferConfiguration teamOfferConfiguration, StripeClient stripeClient,
    UserDatabaseTable userDatabaseTable,
    OrganizationDatabaseTable organizationDatabaseTable,
    @Qualifier("saleMail") TaskwolfMail saleMail
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.offerDatabaseTable = offerDatabaseTable;
    this.bundlePresetRepository = bundlePresetRepository;
    this.teamOfferConfiguration = teamOfferConfiguration;
    this.stripeClient = stripeClient;
    this.userDatabaseTable = userDatabaseTable;
    this.organizationDatabaseTable = organizationDatabaseTable;
    this.saleMail = saleMail;
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

  private static final String OFFER_EMAIL_TITLE = "Offer";
  private static final String OFFER_EMAIL_BODY = "Hey %s,\n" +
    "\n" +
    "we are pleased to inform you that we have put together " +
    "a customized Taskwolf package for you.\n" +
    "\n" +
    "This package is personalized to your needs.\n" +
    "\n" +
    "Click on the link below to view and accept or decline the offer.\n" +
    "\n" +
    "https://taskwolf.net/offer/%s/\n" +
    "\n" +
    "We hope we have been able to meet your requirements.\n" +
    "Here's to good cooperation!";

  private void sendOfferEmail(User user, UUID offerId) {
    saleMail.send(user.email(), OFFER_EMAIL_TITLE,
      String.format(OFFER_EMAIL_BODY, user.name(), offerId.toString()));
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
