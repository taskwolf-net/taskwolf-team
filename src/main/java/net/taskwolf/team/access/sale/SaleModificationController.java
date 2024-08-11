package net.taskwolf.team.access.sale;

import com.google.common.collect.Lists;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.mail.TaskwolfMail;
import net.taskwolf.core.sale.Sale;
import net.taskwolf.core.sale.SaleDatabaseTable;
import net.taskwolf.core.sale.SaleMessageDatabaseTable;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import net.taskwolf.team.member.TeamMember;
import net.taskwolf.team.member.TeamWorkDatabaseTable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.UUID;

@RestController
public final class SaleModificationController extends TeamController {
  private final TaskwolfMail saleMail;
  private final SaleDatabaseTable saleDatabaseTable;
  private final SaleMessageDatabaseTable saleMessageDatabaseTable;
  private final TeamWorkDatabaseTable teamWorkDatabaseTable;

  private SaleModificationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    @Qualifier("saleMail") TaskwolfMail saleMail,
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

  @RequestMapping(path = "/sale/delete/", method = RequestMethod.POST)
  public void deleteSale(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var saleId = body.getUUID("sale");
    memberHasPermission(findMemberId(request), "sale.delete")
      .thenAccept(hasPermission -> saleDatabaseTable.saleExists(saleId)
        .thenAccept(exists -> deleteSale(saleId, hasPermission, exists)));
  }

  private void deleteSale(UUID saleId, boolean hasPermission, boolean exists) {
    if (!hasPermission || !exists) {
      return;
    }
    saleDatabaseTable.findSale(saleId).thenAccept(this::deleteSale);
  }

  private void deleteSale(Sale sale) {
    saleDatabaseTable.deleteSale(sale.requestMessage());
    saleMessageDatabaseTable.deleteSaleMessage(sale.requestMessage());
    for (var message : sale.conversationMessages()) {
      saleMessageDatabaseTable.deleteSaleMessage(message);
    }
  }

  @RequestMapping(path = "/sale/answer/", method = RequestMethod.POST)
  public void answerSale(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
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
    saleMessageDatabaseTable.insertSaleMessage(messageId, "Taskwolf",
      message, System.currentTimeMillis());
    sale.addConversationMessage(messageId);
    sale.resetExpirationTime();
    saleDatabaseTable.updateSale(sale);
    saleMail.send(sale.sender(), "Re: " + sale.title(), message,
      "text/html; charset=utf-8");
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
