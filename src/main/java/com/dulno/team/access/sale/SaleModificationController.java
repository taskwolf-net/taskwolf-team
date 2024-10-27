package com.dulno.team.access.sale;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.mail.Mail;
import com.dulno.core.sale.Sale;
import com.dulno.core.sale.SaleDatabaseTable;
import com.dulno.core.sale.SaleMessageDatabaseTable;
import com.dulno.core.sale.SaleMessageSenderType;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.dulno.team.member.TeamMember;
import com.dulno.team.member.TeamWorkDatabaseTable;
import com.google.common.collect.Lists;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.UUID;

@RestController
public final class SaleModificationController extends TeamController {
  private final Mail saleMail;
  private final SaleDatabaseTable saleDatabaseTable;
  private final SaleMessageDatabaseTable saleMessageDatabaseTable;
  private final TeamWorkDatabaseTable teamWorkDatabaseTable;

  private SaleModificationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    @Qualifier("saleMail") Mail saleMail,
    SaleDatabaseTable saleDatabaseTable,
    SaleMessageDatabaseTable saleMessageDatabaseTable,
    TeamWorkDatabaseTable teamWorkDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.saleMail = saleMail;
    this.saleDatabaseTable = saleDatabaseTable;
    this.saleMessageDatabaseTable = saleMessageDatabaseTable;
    this.teamWorkDatabaseTable = teamWorkDatabaseTable;
  }

  @RequestMapping(path = "/sale/close/", method = RequestMethod.POST)
  public void closeSale(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var saleId = body.getUUID("sale");
    memberHasPermission(findMemberId(request), "sale.close")
      .thenAccept(hasPermission -> saleDatabaseTable.saleExists(saleId)
        .thenAccept(exists -> closeSale(saleId, hasPermission, exists)));
  }

  private void closeSale(UUID saleId, boolean hasPermission, boolean exists) {
    if (!hasPermission || !exists) {
      return;
    }
    saleDatabaseTable.updateSaleStatus(saleId, Sale.Status.CLOSED);
  }

  @RequestMapping(path = "/sale/answer/", method = RequestMethod.POST)
  public void answerSale(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var saleId = body.getUUID("sale");
    findMember(request).thenAccept(teamMember ->
      memberHasPermission(teamMember.id(), "sale.answer")
        .thenAccept(hasPermission -> saleDatabaseTable.saleExists(saleId)
          .thenAccept(exists -> answerSale(teamMember, saleId,
            body.getString("message"), hasPermission, exists))));
  }

  private void answerSale(
    TeamMember teamMember, UUID saleId, String message, boolean hasPermission,
    boolean exists
  ) {
    if (!hasPermission || !exists) {
      return;
    }
    saleDatabaseTable.findSale(saleId).thenAccept(sale ->
      saleMessageDatabaseTable.generateAvailableMessageId().thenAccept(id ->
        answerSale(teamMember, sale, message, id)));
  }

  private void answerSale(
    TeamMember teamMember, Sale sale, String message, UUID messageId
  ) {
    sale.resetExpirationTime();
    saleDatabaseTable.updateSale(sale);
    saleMail.send(sale.sender(), "Re: " + sale.title(), message,
      "text/html; charset=utf-8")
      .thenAccept(publicId -> saleMessageDatabaseTable.insertSaleMessage(
        messageId, publicId, sale.id(), teamMember.id().toString(),
        SaleMessageSenderType.TEAM, message, System.currentTimeMillis()));
    teamWorkDatabaseTable.hasRequestReplies(teamMember.id())
      .thenAccept(exists -> addTeamMemberReply(teamMember, exists));
  }

  private void addTeamMemberReply(TeamMember teamMember, boolean workExists) {
    if (!workExists) {
      teamWorkDatabaseTable.insertRequestReplies(teamMember.id(),
        Lists.newArrayList(System.currentTimeMillis()));
      return;
    }
    teamWorkDatabaseTable.addRequestReply(teamMember.id(),
      System.currentTimeMillis());
  }
}
