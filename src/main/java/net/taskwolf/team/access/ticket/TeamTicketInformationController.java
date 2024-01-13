package net.taskwolf.team.access.ticket;

import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import net.taskwolf.core.iterator.AsyncIterator;
import net.taskwolf.core.ticket.Ticket;
import net.taskwolf.core.ticket.TicketDatabaseTable;
import net.taskwolf.core.ticket.TicketMessage;
import net.taskwolf.core.ticket.TicketMessageDatabaseTable;
import net.taskwolf.core.user.ProfilePictureDatabaseTable;
import net.taskwolf.core.user.User;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.access.TeamController;
import org.springframework.beans.factory.annotation.Qualifier;
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
  private final ProfilePictureDatabaseTable profilePictureDatabaseTable;
  private final User taskwolfUser;

  private TeamTicketInformationController(
    Key secretKey, UserDatabaseTable userDatabaseTable,
    TicketDatabaseTable ticketDatabaseTable,
    TicketMessageDatabaseTable ticketMessageDatabaseTable,
    ProfilePictureDatabaseTable profilePictureDatabaseTable,
    @Qualifier("taskwolfUser") User taskwolfUser
  ) {
    super(secretKey, userDatabaseTable);
    this.ticketDatabaseTable = ticketDatabaseTable;
    this.ticketMessageDatabaseTable = ticketMessageDatabaseTable;
    this.profilePictureDatabaseTable = profilePictureDatabaseTable;
    this.taskwolfUser = taskwolfUser;
  }

  @RequestMapping(path = "/team/tickets/find/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findTickets(
    HttpServletRequest request
  ) {
    if (!isAuthenticatedTeamMember(request)) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    ticketDatabaseTable.findAllTickets().thenAccept(tickets ->
      futureResponse.complete(Map.of("tickets", tickets.stream()
        .map(this::superficialTicketInformation).toList())));
    return futureResponse;
  }

  @RequestMapping(path = "/team/ticket/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findTicket(
    HttpServletRequest request, @RequestBody Map<String, Object> input
  ) {
    if (!isAuthenticatedTeamMember(request)) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    var ticketId = UUID.fromString((String) input.get("ticket"));
    ticketDatabaseTable.ticketExists(ticketId).thenAccept(exists ->
      findTicket(ticketId, exists).thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> findTicket(
    UUID ticketId, boolean exists
  ) {
    if (!exists) {
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
      ticketMessageDatabaseTable::findTicketMessage, ticket.messages().size(),
      messages -> profilePictureDatabaseTable.findProfilePicture(ticket.creator())
        .thenAccept(picture -> detailedTicketInformation(ticket, picture, messages)
          .thenAccept(futureResponse::complete)));
    return futureResponse;
  }

  private Map<String, Object> superficialTicketInformation(
    Ticket ticket
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", ticket.id());
    information.put("title", ticket.title());
    information.put("type", ticket.type());
    information.put("status", ticket.status());
    return information;
  }

  private CompletableFuture<Map<String, Object>> detailedTicketInformation(
    Ticket ticket, String creatorProfilePicture, List<TicketMessage> messages
  ) {
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    AsyncIterator.execute(messages, this::messageInformation, messages.size(),
      information -> futureResponse.complete(assemblyDetailedTicketInformation(
        ticket, creatorProfilePicture, information)));
    return futureResponse;
  }

  private Map<String, Object> assemblyDetailedTicketInformation(
    Ticket ticket, String creatorProfilePicture,
    List<Map<String, Object>> messageInformation
  ) {
    messageInformation.sort(Comparator.comparing(entry ->
      (long) entry.get("rawTime")));
    var information = superficialTicketInformation(ticket);
    information.put("creatorProfilePicture", creatorProfilePicture);
    information.put("messages", messageInformation);
    return information;
  }

  private CompletableFuture<Map<String, Object>> messageInformation(
    TicketMessage message
  ) {
    return userDatabaseTable().findUser(message.author()).thenApply(author ->
      assemblyMessageInformation(message, author));
  }

  private Map<String, Object> assemblyMessageInformation(
    TicketMessage message, User author
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", message.id());
    information.put("author", author.name());
    information.put("teamWritten", author.id().equals(taskwolfUser.id()));
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
