package net.taskwolf.team.access.offer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
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

  private OfferModificationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, OfferDatabaseTable offerDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.offerDatabaseTable = offerDatabaseTable;
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
