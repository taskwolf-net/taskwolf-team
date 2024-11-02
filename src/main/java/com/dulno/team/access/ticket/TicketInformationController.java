package com.dulno.team.access.ticket;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.iterator.AsyncIterator;
import com.dulno.core.ticket.Ticket;
import com.dulno.core.ticket.TicketDatabaseTable;
import com.dulno.core.ticket.TicketMessage;
import com.dulno.core.ticket.TicketMessageDatabaseTable;
import com.dulno.core.user.User;
import com.dulno.core.user.UserDatabaseTable;
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

@RestController
public final class TicketInformationController extends TeamController {
  private final TicketDatabaseTable ticketDatabaseTable;
  private final TicketMessageDatabaseTable ticketMessageDatabaseTable;
  private final UserDatabaseTable userDatabaseTable;

  private TicketInformationController(
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

  @RequestMapping(path = "/tickets/pending/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> countPendingTickets() {
    return ticketDatabaseTable.countPendingTickets().thenApply(tickets ->
      Map.of("tickets", tickets));
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
    return ticketDatabaseTable.findOpenTickets()
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
        ticketDatabaseTable.updateTicketStatus(ticket.id(), Ticket.Status.CLOSED);
      }
    }
    tickets.removeAll(sortedOut);
    return tickets;
  }

  @RequestMapping(path = "/ticket/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findTicket(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
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
    var teamWritten = message.authorType().isTeam();
    var futureAuthorName = teamWritten ?
      teamDatabaseTable().findTeamMember(message.author()).thenApply(TeamMember::name) :
      userDatabaseTable.findUser(message.author()).thenApply(User::name);
    return futureAuthorName.thenApply(author ->
      assemblyMessageInformation(message, author, teamWritten));
  }

  private Map<String, Object> assemblyMessageInformation(
    TicketMessage message, String authorName, boolean teamWritten
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", message.id());
    information.put("author", authorName);
    information.put("teamWritten", teamWritten);
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
