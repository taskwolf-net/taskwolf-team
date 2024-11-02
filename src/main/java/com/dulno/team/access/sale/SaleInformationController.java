package com.dulno.team.access.sale;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.iterator.AsyncIterator;
import com.dulno.core.mail.Mail;
import com.dulno.core.mail.MailMessage;
import com.dulno.core.sale.*;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.dulno.team.member.TeamMember;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.internet.InternetAddress;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
public final class SaleInformationController extends TeamController {
  private final Mail saleMail;
  private final SaleDatabaseTable saleDatabaseTable;
  private final SaleMessageDatabaseTable saleMessageDatabaseTable;

  private SaleInformationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, @Qualifier("saleMail") Mail saleMail,
    SaleDatabaseTable saleDatabaseTable,
    SaleMessageDatabaseTable saleMessageDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.saleMail = saleMail;
    this.saleDatabaseTable = saleDatabaseTable;
    this.saleMessageDatabaseTable = saleMessageDatabaseTable;
  }

  @RequestMapping(path = "/sales/pending/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> countPendingTickets() {
    return saleDatabaseTable.countPendingSales().thenApply(sales ->
      Map.of("sales", sales));
  }

  @RequestMapping(path = "/sales/find/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findSales(
    HttpServletRequest request
  ) {
    return memberHasPermission(findMemberId(request), "sales.find")
      .thenCompose(this::findSales);
  }

  private CompletableFuture<Map<String, Object>> findSales(
    boolean hasPermission
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return findOpenSales().thenCompose(this::assemblySales);
  }

  private CompletableFuture<Map<String, Object>> assemblySales(
    Map<Sale, List<SaleMessage>> sales
  ) {
    return AsyncIterator.execute(Lists.newArrayList(sales.entrySet()),
        entry -> saleInformation(entry.getKey(), entry.getValue()))
      .thenApply(information -> Map.of("sales", information));
  }

  @RequestMapping(path = "/sale/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findSale(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var saleId = body.getUUID("sale");
    return memberHasPermission(findMemberId(request), "sale.find")
      .thenCompose(hasPermission -> saleDatabaseTable.saleExists(saleId)
        .thenCompose(exists -> findSale(saleId, hasPermission, exists)));
  }

  private CompletableFuture<Map<String, Object>> findSale(
    UUID saleId, boolean hasPermission, boolean exists
  ) {
    if (!hasPermission || !exists) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return saleDatabaseTable.findSale(saleId).thenCompose(sale ->
      saleMessageDatabaseTable.findMessagesOfSale(sale.id())
        .thenCompose(messages ->  saleInformation(sale, messages)));
  }

  private CompletableFuture<Map<String, Object>> saleInformation(
    Sale sale, List<SaleMessage> messages
  ) {
    var sortedMessages = messages.stream()
      .sorted(Comparator.comparing(SaleMessage::time)).toList();
    return AsyncIterator.execute(messages, this::messageInformation)
      .thenApply(information -> information.stream()
        .sorted(Comparator.comparing(entry -> (long) entry.get("rawTime"))).toList())
      .thenApply(information -> assemblySaleInformation(sale,
        sortedMessages, information));
  }

  private Map<String, Object> assemblySaleInformation(
    Sale sale, List<SaleMessage> messages,
    List<Map<String, Object>> messageInformation
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", sale.id());
    information.put("title", sale.title());
    information.put("status", sale.status());
    information.put("sender", sale.sender());
    information.put("firstName", sale.firstName());
    information.put("lastName", sale.lastName());
    information.put("phoneNumber", sale.phoneNumber());
    information.put("country", sale.country());
    information.put("companyName", sale.companyName());
    information.put("companySize", sale.companySize());
    information.put("companyRole", sale.companyRole());
    information.put("date", formatTime(messages.get(messages.size() - 1).time()));
    information.put("messages", messageInformation);
    return information;
  }

  private CompletableFuture<Map<String, Object>> messageInformation(
    SaleMessage message
  ) {
    var teamWritten = message.senderType().isTeam();
    var futureSender = teamWritten ?
      teamDatabaseTable().findTeamMember(UUID.fromString(message.sender()))
        .thenApply(TeamMember::name) :
      CompletableFuture.completedFuture(message.sender());
    return futureSender.thenApply(sender ->
      assemblyMessageInformation(message, sender, teamWritten));
  }

  private Map<String, Object> assemblyMessageInformation(
    SaleMessage message, String sender, boolean teamWritten
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", message.id());
    information.put("sender", sender);
    information.put("body", message.content());
    information.put("teamWritten", teamWritten);
    information.put("time", formatTime(message.time()));
    information.put("rawTime", message.time());
    return information;
  }

  public CompletableFuture<Map<Sale, List<SaleMessage>>> findOpenSales() {
    var futureResponse = new CompletableFuture<Map<Sale, List<SaleMessage>>>();
    saleMail.inboxAndFlush().thenAccept(inbox -> saveInbox(new ArrayList<>(inbox))
      .thenAccept(value -> findAllSales().thenAccept(futureResponse::complete)));
    return futureResponse;
  }

  private CompletableFuture<Void> saveInbox(List<MailMessage> inbox) {
    if (inbox.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }
    inbox.sort(Comparator.comparing(message -> message.receiveDate().getTime()));
    var futureResponse = new CompletableFuture<Void>();
    var firstElement = inbox.remove(0);
    saveSaleMail(firstElement, inbox, () -> futureResponse.complete(null));
    return futureResponse;
  }

  private void saveSaleMail(
    MailMessage incomingMessage, List<MailMessage> inbox,
    Runnable futureResponse
  ) {
    saleMessageDatabaseTable.saleMessageExists(incomingMessage.inReplyTo())
      .thenCompose(exists -> findSale(incomingMessage.inReplyTo(), exists))
      .thenAccept(sale -> saveSaleMail(incomingMessage, inbox, futureResponse,
        sale));
  }

  private CompletableFuture<Sale> findSale(String inReplyTo, boolean exists) {
    if (!exists) {
      return CompletableFuture.completedFuture(null);
    }
    return saleMessageDatabaseTable.findSaleMessage(inReplyTo)
      .thenCompose(message -> saleDatabaseTable.findSale(message.saleId()));
  }

  private void saveSaleMail(
    MailMessage incomingMessage, List<MailMessage> inbox,
    Runnable futureResponse, Sale sale
  ) {
    if (sale == null) {
      saveNextSale(inbox, futureResponse);
      return;
    }
    saveSaleMailMessage(sale.id(), incomingMessage)
      .thenAccept(value -> saveSaleMail(sale, inbox, futureResponse));
  }

  private CompletableFuture<Void> saveSaleMailMessage(
    UUID saleId, MailMessage message
  ) {
    var sender = ((InternetAddress) message.sender()).getAddress();
    return saleMessageDatabaseTable.generateAvailableMessageId()
      .thenAccept(id -> saleMessageDatabaseTable.insertSaleMessage(
        SaleMessage.create(id, message.messageId(), saleId, sender,
          SaleMessageSenderType.USER, message.body(),
          message.receiveDate().getTime())));
  }

  private void saveSaleMail(
    Sale parentMail, List<MailMessage> inbox, Runnable futureResponse
  ) {
    parentMail.disableExpirationTime();
    saleDatabaseTable.updateSale(parentMail).thenAccept(value ->
      saveNextSale(inbox, futureResponse));
  }

  private void saveNextSale(
    List<MailMessage> inbox, Runnable futureResponse
  ) {
    if (inbox.isEmpty()) {
      futureResponse.run();
      return;
    }
    var firstElement = inbox.remove(0);
    saveSaleMail(firstElement, inbox, futureResponse);
  }

  private CompletableFuture<Map<Sale, List<SaleMessage>>> findAllSales() {
    var futureResponse = new CompletableFuture<Map<Sale, List<SaleMessage>>>();
    saleDatabaseTable.findOpenSales().thenApply(this::sortOutExpiredSales)
      .thenAccept(sales -> AsyncIterator.execute(sales, sale ->
          saleMessageDatabaseTable.findMessagesOfSale(sale.id())
            .thenApply(messages -> new AbstractMap.SimpleEntry<>(sale, messages)))
        .thenAccept(entries -> futureResponse.complete(entries.stream()
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))));
    return futureResponse;
  }

  private List<Sale> sortOutExpiredSales(List<Sale> sales) {
    var sortedOut = Lists.<Sale>newArrayList();
    var currentTime = System.currentTimeMillis();
    for (var sale : sales) {
      if (sale.expirationTime() == -1) {
        continue;
      }
      sortedOut.add(sale);
      if (currentTime >= sale.expirationTime()) {
        saleDatabaseTable.updateSaleStatus(sale.id(), Sale.Status.CLOSED);
      }
    }
    sales.removeAll(sortedOut);
    return sales;
  }

  private String formatTime(long time) {
    var calendar = Calendar.getInstance();
    calendar.setTimeInMillis(time);
    return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(calendar.getTime());
  }
}
