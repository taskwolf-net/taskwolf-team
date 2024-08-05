package net.taskwolf.team.access.ticket;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.iterator.AsyncIterator;
import net.taskwolf.core.ticket.Ticket;
import net.taskwolf.core.ticket.TicketDatabaseTable;
import net.taskwolf.core.ticket.TicketMessage;
import net.taskwolf.core.ticket.TicketMessageDatabaseTable;
import net.taskwolf.core.user.User;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
public final class TeamTicketInformationController extends TeamController {
  private final TicketDatabaseTable ticketDatabaseTable;
  private final TicketMessageDatabaseTable ticketMessageDatabaseTable;
  private final UserDatabaseTable userDatabaseTable;

  private TeamTicketInformationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, TicketDatabaseTable ticketDatabaseTable,
    TicketMessageDatabaseTable ticketMessageDatabaseTable,
    UserDatabaseTable userDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.ticketDatabaseTable = ticketDatabaseTable;
    this.ticketMessageDatabaseTable = ticketMessageDatabaseTable;
    this.userDatabaseTable = userDatabaseTable;
  }

  @RequestMapping(path = "/tickets/find/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findTickets(
    HttpServletRequest request
  ) {
    return memberHasPermission(findMemberId(request), "tickets.find")
      .thenCompose(this::findTickets);
  }

  private CompletableFuture<Map<String, Object>> findTickets(
    boolean hasPermission
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return ticketDatabaseTable.findAllTickets()
      .thenApply(tickets -> sortOutExpiredTickets(new ArrayList<>(tickets)))
      .thenCompose(tickets -> AsyncIterator.execute(tickets, this::findTicket)
        .thenApply(information -> Map.of("tickets", information)));
  }

  private List<Ticket> sortOutExpiredTickets(List<Ticket> tickets) {
    var sortedOut = Lists.<Ticket>newArrayList();
    var currentTime = System.currentTimeMillis();
    for (var ticket : tickets) {
      if (ticket.expirationTime() == -1) {
        continue;
      }
      sortedOut.add(ticket);
      if (currentTime >= ticket.expirationTime()) {
        deleteTicket(ticket);
      }
    }
    tickets.removeAll(sortedOut);
    return tickets;
  }

  private void deleteTicket(Ticket ticket) {
    ticketDatabaseTable.deleteTicket(ticket.id());
    for (var message : ticket.messages()) {
      ticketMessageDatabaseTable.deleteTicketMessage(message);
    }
  }

  @RequestMapping(path = "/ticket/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findTicket(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    var ticketId = body.getUUID("ticket");
    memberHasPermission(findMemberId(request), "ticket.find")
      .thenAccept(hasPermission -> ticketDatabaseTable.ticketExists(ticketId)
        .thenAccept(exists -> findTicket(ticketId, hasPermission, exists)
          .thenAccept(futureResponse::complete)));
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> findTicket(
    UUID ticketId, boolean hasPermission, boolean exists
  ) {
    if (!hasPermission || !exists) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    ticketDatabaseTable.findTicket(ticketId).thenAccept(ticket ->
      findTicket(ticket).thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> findTicket(Ticket ticket) {
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    AsyncIterator.execute(ticket.messages(),
      ticketMessageDatabaseTable::findTicketMessage).thenAccept(
      messages -> userDatabaseTable.findUser(ticket.creator()).thenAccept(
        creator -> ticketInformation(ticket, creator, messages)
          .thenAccept(futureResponse::complete)));
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> ticketInformation(
    Ticket ticket, User creator, List<TicketMessage> messages
  ) {
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    AsyncIterator.execute(messages, this::messageInformation).thenAccept(
      information -> futureResponse.complete(assemblyTicketInformation(
        ticket, creator, information)));
    return futureResponse;
  }

  private Map<String, Object> assemblyTicketInformation(
    Ticket ticket, User creator, List<Map<String, Object>> messageInformation
  ) {
    messageInformation.sort(Comparator.comparing(entry ->
      (long) entry.get("rawTime")));
    var information = Maps.<String, Object>newHashMap();
    information.put("id", ticket.id());
    information.put("title", ticket.title());
    information.put("creator", creator.name());
    information.put("type", ticket.type());
    information.put("status", ticket.status());
    information.put("date", messageInformation.get(messageInformation.size() - 1)
      .get("time"));
    information.put("messages", messageInformation);
    return information;
  }

  private CompletableFuture<Map<String, Object>> messageInformation(
    TicketMessage message
  ) {
    return userDatabaseTable.findUser(message.author()).thenApply(author ->
      assemblyMessageInformation(message, author));
  }

  private static final UUID TASKWOLF_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private Map<String, Object> assemblyMessageInformation(
    TicketMessage message, User author
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", message.id());
    information.put("author", author.name());
    information.put("teamWritten", author.id().equals(TASKWOLF_USER_ID));
    information.put("message", message.message());
    information.put("rawTime", message.time());
    information.put("time", formatTime(message.time()));
    return information;
  }

  private String formatTime(long time) {
    var calendar = Calendar.getInstance();
    calendar.setTimeInMillis(time);
    return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(calendar.getTime());
  }
}
