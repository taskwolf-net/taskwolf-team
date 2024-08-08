package net.taskwolf.team.access.sale;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.iterator.AsyncIterator;
import net.taskwolf.core.sale.Sale;
import net.taskwolf.core.sale.SaleDatabaseTable;
import net.taskwolf.core.sale.SaleMessage;
import net.taskwolf.core.sale.SaleMessageDatabaseTable;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
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
    return findAllSupportSales().thenApply(this::assemblySales);
  }

  @RequestMapping(path = "/sale/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findSale(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var saleId = body.getUUID("sale");
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    memberHasPermission(findMemberId(request), "sale.find")
      .thenAccept(hasPermission -> saleDatabaseTable.saleExists(saleId)
        .thenAccept(exists -> findSale(saleId, hasPermission, exists)
          .thenAccept(futureResponse::complete)));
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> findSale(
    UUID saleId, boolean hasPermission, boolean exists
  ) {
    if (!hasPermission || !exists) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    saleDatabaseTable.findSale(saleId).thenAccept(sale ->
      findSaleMessages(sale).thenAccept(messages ->
        futureResponse.complete(assemblySale(sale, messages))));
    return futureResponse;
  }

  private Map<String, Object> assemblySale(
    Sale sale, List<SaleMessage> messages
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
    messages.sort(Comparator.comparing(SaleMessage::time));
    information.put("date", formatTime(messages.get(messages.size() - 1).time()));
    var messagesInformation = Lists.<Map<String, Object>>newArrayList();
    for (var message : messages) {
      var messageInformation = Maps.<String, Object>newHashMap();
      messageInformation.put("id", message.id());
      messageInformation.put("sender", message.sender());
      messageInformation.put("body", message.content());
      messageInformation.put("teamWritten", message.sender()
        .equalsIgnoreCase("Taskwolf"));
      messageInformation.put("time", formatTime(message.time()));
      messagesInformation.add(messageInformation);
    }
    information.put("messages", messagesInformation);
    return information;
  }

  private Map<String, Object> assemblySales(
    Map<Sale, List<SaleMessage>> sales
  ) {
    var information = Lists.<Map<String, Object>>newArrayList();
    for (var entry : sales.entrySet()) {
      information.add(assemblySale(entry.getKey(), entry.getValue()));
    }
    return Map.of("sales", information);
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
