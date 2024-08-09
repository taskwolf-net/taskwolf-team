package net.taskwolf.team.access.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.stripe.StripeClient;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentListParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.organization.Organization;
import net.taskwolf.core.organization.OrganizationDatabaseTable;
import net.taskwolf.core.stripe.StripeDatabaseTable;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
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
public final class EntityPaymentController extends EntityStripeController {
  private EntityPaymentController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    StripeDatabaseTable stripeDatabaseTable,
    StripeClient stripeClient, UserDatabaseTable userDatabaseTable,
    OrganizationDatabaseTable organizationDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable,
      stripeDatabaseTable, stripeClient, userDatabaseTable,
      organizationDatabaseTable);
  }

  @RequestMapping(path = "/entity/payments/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findEntityPayments(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var entityId = body.getUUID("entity");
    return memberHasPermission(findMemberId(request), "entity.payments.find")
      .thenCompose(hasPermission -> findStripeTarget(entityId)
        .thenCompose(target -> stripeDatabaseTable().stripeAccountExistsByTarget(target)
          .thenComposeAsync(stripeExists -> findEntityPayments(target,
            hasPermission, stripeExists))));
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
    return stripeDatabaseTable().findStripeAccountByTarget(target)
      .thenApplyAsync(account -> findPayments(account.accountId()));
  }

  private Map<String, Object> findPayments(String accountId) {
    try {
      var parameters = PaymentIntentListParams.builder()
        .setCustomer(accountId).build();
      var payments = stripeClient().paymentIntents().list(parameters);
      var information = Lists.newArrayList();
      for (var payment : payments.getData()) {
        var invoice = stripeClient().invoices().retrieve(payment.getInvoice());
        information.add(assemblyPaymentInformation(payment, invoice));
      }
      return Map.of("payments", information);
    } catch (Exception exception) {
      exception.printStackTrace();
      return Maps.newHashMap();
    }
  }

  private Map<String, Object> assemblyPaymentInformation(
    PaymentIntent payment, Invoice invoice
  ) throws Exception {
    var information = Maps.<String, Object>newHashMap();
    information.put("time", formatTime(payment.getCreated() * 1000));
    information.put("amount", payment.getAmount() / 100D);
    information.put("method", stripeClient().paymentMethods()
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