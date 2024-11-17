package com.dulno.team.access.entity;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.error.ErrorRepository;
import com.dulno.core.stripe.StripeDatabaseTable;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.stripe.StripeClient;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentListParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class EntityPaymentController extends TeamController {
  private final StripeDatabaseTable stripeDatabaseTable;
  private final StripeClient stripeClient;
  private final ErrorRepository errorRepository;

  private EntityPaymentController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, StripeDatabaseTable stripeDatabaseTable,
    StripeClient stripeClient, ErrorRepository errorRepository
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.stripeDatabaseTable = stripeDatabaseTable;
    this.stripeClient = stripeClient;
    this.errorRepository = errorRepository;
  }

  @RequestMapping(path = "/entity/payments/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findEntityPayments(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var entityId = body.getUUID("entity");
    return memberHasPermission(findMemberId(request), "entity.payments.find")
      .thenCompose(hasPermission ->
        stripeDatabaseTable.stripeAccountExistsByTarget(entityId)
          .thenComposeAsync(stripeExists -> findEntityPayments(entityId,
            hasPermission, stripeExists)));
  }

  private CompletableFuture<Map<String, Object>> findEntityPayments(
    UUID target, boolean hasPermission, boolean stripeAccountExists
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    if (!stripeAccountExists) {
      return CompletableFuture.completedFuture(Map.of("found", false));
    }
    return stripeDatabaseTable.findStripeAccountByTarget(target)
      .thenApplyAsync(account -> findPayments(account.accountId()));
  }

  private Map<String, Object> findPayments(String accountId) {
    try {
      var parameters = PaymentIntentListParams.builder()
        .setCustomer(accountId).build();
      var payments = stripeClient.paymentIntents().list(parameters);
      var information = Lists.newArrayList();
      for (var payment : payments.getData()) {
        var invoice = stripeClient.invoices().retrieve(payment.getInvoice());
        information.add(assemblyPaymentInformation(payment, invoice));
      }
      return Map.of("payments", information);
    } catch (Exception exception) {
      errorRepository.processError(exception);
      return Maps.newHashMap();
    }
  }

  private Map<String, Object> assemblyPaymentInformation(
    PaymentIntent payment, Invoice invoice
  ) throws Exception {
    var information = Maps.<String, Object>newHashMap();
    information.put("time", formatTime(payment.getCreated() * 1000));
    information.put("amount", payment.getAmount() / 100D);
    information.put("method", stripeClient.paymentMethods()
      .retrieve(payment.getPaymentMethod()).getType());
    information.put("status", payment.getStatus());
    information.put("invoice", invoice.getInvoicePdf());
    return information;
  }

  private String formatTime(long time) {
    var calendar = Calendar.getInstance();
    calendar.setTimeInMillis(time);
    return new SimpleDateFormat("dd.MM.yyyy").format(calendar.getTime());
  }
}