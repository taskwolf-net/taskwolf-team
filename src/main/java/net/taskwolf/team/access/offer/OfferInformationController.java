package net.taskwolf.team.access.offer;

import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.offer.Offer;
import net.taskwolf.core.offer.OfferDatabaseTable;
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
public final class OfferInformationController extends TeamController {
  private final OfferDatabaseTable offerDatabaseTable;

  private OfferInformationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, OfferDatabaseTable offerDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.offerDatabaseTable = offerDatabaseTable;
  }

  @RequestMapping(path = "/offers/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findOffers(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var targetId = body.getUUID("target");
    return memberHasPermission(findMemberId(request), "offers.find")
      .thenCompose(hasPermission -> findOffers(targetId, hasPermission));
  }

  private CompletableFuture<Map<String, Object>> findOffers(
    UUID targetId, boolean hasPermission
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return offerDatabaseTable.findOffersByTarget(targetId)
      .thenApply(offers -> Map.of("offers",
        offers.stream().map(this::assemblyOfferInformation).toList()));
  }

  @RequestMapping(path = "/offer/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findOffer(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var offerId = body.getUUID("offer");
    return memberHasPermission(findMemberId(request), "offer.find")
      .thenCompose(hasPermission -> offerDatabaseTable.offerExists(offerId)
        .thenCompose(exists -> findOffer(offerId, hasPermission, exists)));
  }

  private CompletableFuture<Map<String, Object>> findOffer(
    UUID offerId, boolean hasPermission, boolean offerExists
  ) {
    if (!hasPermission || !offerExists) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return offerDatabaseTable.findOffer(offerId)
      .thenApply(this::assemblyOfferInformation);
  }

  private Map<String, Object> assemblyOfferInformation(Offer offer) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", offer.id().toString());
    information.put("status", offer.offerStatus().toString());
    information.put("type", offer.bundleType().toString());
    information.put("class", offer.bundleClass().toString());
    information.put("runtime", offer.bundleRuntime().toString());
    information.put("price", offer.price());
    information.put("workflowAccess", offer.workflowAccess());
    information.put("workflowNumberLimit", offer.workflowNumberLimit());
    information.put("workflowOperationLimit", offer.workflowOperationLimit());
    information.put("workflowTemplateAccess", offer.workflowTemplateAccess());
    information.put("databaseAccess", offer.databaseAccess());
    information.put("databaseNumberLimit", offer.databaseNumberLimit());
    information.put("databaseDataLimit", new DecimalFormat("#.#").format(
      offer.databaseDataLimit()));
    information.put("webhookAccess", offer.webhookAccess());
    information.put("webhookNumberLimit", offer.webhookNumberLimit());
    information.put("organizationAccess", offer.organizationAccess());
    information.put("organizationMemberLimit", offer.organizationMemberLimit());
    information.put("organizationTeamLimit", offer.organizationTeamLimit());
    information.put("deviceAccess", offer.deviceAccess());
    information.put("accountsAccess", offer.accountsAccess());
    information.put("accountsNumberLimit", offer.accountsNumberLimit());
    return information;
  }
}
