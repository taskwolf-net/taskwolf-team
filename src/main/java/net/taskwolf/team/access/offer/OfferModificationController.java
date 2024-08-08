package net.taskwolf.team.access.offer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.bundle.*;
import net.taskwolf.core.offer.OfferDatabaseTable;
import net.taskwolf.core.offer.OfferStatus;
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
public final class OfferModificationController extends TeamController {
  private final OfferDatabaseTable offerDatabaseTable;
  private final BundlePresetRepository bundlePresetRepository;

  private OfferModificationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, OfferDatabaseTable offerDatabaseTable,
    BundlePresetRepository bundlePresetRepository
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.offerDatabaseTable = offerDatabaseTable;
    this.bundlePresetRepository = bundlePresetRepository;
  }

  @RequestMapping(path = "/offer/price/suggestion/", method = RequestMethod.POST)
  public void createOfferPriceSuggestion(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var targetId = body.getUUID("target");
    memberHasPermission(findMemberId(request), "offer.price.suggestion")
      .thenAccept(hasPermission -> createOffer(targetId, body, hasPermission));
  }

  private void createOfferPriceSuggestion(
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
  }

  @RequestMapping(path = "/offer/create/", method = RequestMethod.POST)
  public void createOffer(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var targetId = body.getUUID("target");
    memberHasPermission(findMemberId(request), "offer.create")
      .thenAccept(hasPermission -> createOffer(targetId, body, hasPermission));
  }

  private void createOffer(
    UUID targetId, TaskwolfRequestBody body, boolean hasPermission
  ) {
    if (!hasPermission) {
      return;
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
