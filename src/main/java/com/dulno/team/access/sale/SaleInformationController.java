package com.dulno.team.access.sale;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.iterator.AsyncIterator;
import com.dulno.core.sale.Sale;
import com.dulno.core.sale.SaleDatabaseTable;
import com.dulno.core.sale.SaleMessage;
import com.dulno.core.sale.SaleMessageDatabaseTable;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.dulno.team.member.TeamMember;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public final class SaleInformationController extends TeamController {
  private final SaleDatabaseTable saleDatabaseTable;
  private final SaleMessageDatabaseTable saleMessageDatabaseTable;

  private SaleInformationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, SaleDatabaseTable saleDatabaseTable,
    SaleMessageDatabaseTable saleMessageDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.saleDatabaseTable = saleDatabaseTable;
    this.saleMessageDatabaseTable = saleMessageDatabaseTable;
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
    return findAllSupportSales().thenCompose(this::assemblySales);
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
    return saleDatabaseTable.findSale(saleId)
      .thenCompose(sale -> findSaleMessages(sale)
        .thenCompose(messages -> saleInformation(sale, messages)));
  }

  private CompletableFuture<Map<String, Object>> saleInformation(
    Sale sale, List<SaleMessage> messages
  ) {
    messages.sort(Comparator.comparing(SaleMessage::time));
    return AsyncIterator.execute(messages, this::messageInformation)
      .thenApply(information -> assemblySaleInformation(sale,
        messages, information));
  }

  private Map<String, Object> assemblySaleInformation(
    Sale sale, List<SaleMessage> messages,
    List<Map<String, Object>> messageInformation
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", sale.requestMessage());
    information.put("title", sale.title());
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
    return information;
  }

  private CompletableFuture<Map<Sale, List<SaleMessage>>> findAllSupportSales() {
    var futureResponse = new CompletableFuture<Map<Sale, List<SaleMessage>>>();
    saleDatabaseTable.findAllSales().thenApply(this::sortOutExpiredSales)
      .thenAccept(sales -> AsyncIterator.execute(sales, sale -> findSaleMessages(sale)
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
        deleteSale(sale);
      }
    }
    sales.removeAll(sortedOut);
    return sales;
  }

  private void deleteSale(Sale sale) {
    saleDatabaseTable.deleteSale(sale.requestMessage());
    saleMessageDatabaseTable.deleteSaleMessage(sale.requestMessage());
    for (var message : sale.conversationMessages()) {
      saleMessageDatabaseTable.deleteSaleMessage(message);
    }
  }

  private CompletableFuture<List<SaleMessage>> findSaleMessages(
    Sale sale
  ) {
    var futureResponse = new CompletableFuture<List<SaleMessage>>();
    AsyncIterator.execute(Stream.concat(sale.conversationMessages().stream(),
          Stream.of(sale.requestMessage())).collect(Collectors.toList()),
        saleMessageDatabaseTable::findSaleMessage)
      .thenAccept(futureResponse::complete);
    return futureResponse;
  }

  private String formatTime(long time) {
    var calendar = Calendar.getInstance();
    calendar.setTimeInMillis(time);
    return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(calendar.getTime());
  }
}
