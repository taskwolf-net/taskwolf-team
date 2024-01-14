package net.taskwolf.team.access.ticket;

import jakarta.servlet.http.HttpServletRequest;
import net.taskwolf.core.ticket.Ticket;
import net.taskwolf.core.ticket.TicketDatabaseTable;
import net.taskwolf.core.ticket.TicketMessage;
import net.taskwolf.core.ticket.TicketMessageDatabaseTable;
import net.taskwolf.core.user.User;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.access.TeamController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.UUID;

@RestController
public final class TeamTicketModificationController extends TeamController {
  private final TicketDatabaseTable ticketDatabaseTable;
  private final TicketMessageDatabaseTable ticketMessageDatabaseTable;
  private final User taskwolfUser;

  private TeamTicketModificationController(
    Key secretKey, UserDatabaseTable userDatabaseTable,
    TicketDatabaseTable ticketDatabaseTable,
    TicketMessageDatabaseTable ticketMessageDatabaseTable,
    @Qualifier("taskwolfUser") User taskwolfUser
  ) {
    super(secretKey, userDatabaseTable);
    this.ticketDatabaseTable = ticketDatabaseTable;
    this.ticketMessageDatabaseTable = ticketMessageDatabaseTable;
    this.taskwolfUser = taskwolfUser;
  }

  @RequestMapping(path = "/team/ticket/answer/", method = RequestMethod.POST)
  public void answerTicket(
    HttpServletRequest request, @RequestBody Map<String, Object> input
  ) {
    if (!isAuthenticatedTeamMember(request)) {
      return;
    }
    var ticketId = UUID.fromString((String) input.get("ticket"));
    var message = (String) input.get("message");
    ticketDatabaseTable.ticketExists(ticketId).thenAccept(exists ->
      answerTicket(ticketId, message, exists));
  }

  private void answerTicket(UUID ticketId, String message, boolean exists) {
    if (!exists) {
      return;
    }
    ticketDatabaseTable.findTicket(ticketId).thenAccept(ticket ->
      ticketMessageDatabaseTable.generateAvailableTicketMessageId().thenAccept(id ->
        answerTicket(ticket, message, id)));
  }

  private void answerTicket(Ticket ticket, String message, UUID messageId) {
    ticket.addMessage(messageId);
    ticket.resetExpirationTime();
    ticketDatabaseTable.updateTicket(ticket);
    ticketMessageDatabaseTable.insertTicketMessage(TicketMessage.create(
      messageId, ticket.id(), taskwolfUser.id(), message,
      System.currentTimeMillis()));
  }

  @RequestMapping(path = "/team/ticket/message/delete/", method = RequestMethod.POST)
  public void deleteTicketMessage(
    HttpServletRequest request, @RequestBody Map<String, Object> input
  ) {
    if (!isAuthenticatedTeamMember(request)) {
      return;
    }
    var ticketId = UUID.fromString((String) input.get("ticket"));
    var messageId = UUID.fromString((String) input.get("message"));
    ticketDatabaseTable.ticketExists(ticketId).thenAccept(exists ->
      deleteTicketMessage(ticketId, messageId, exists));
  }

  private void deleteTicketMessage(UUID ticketId, UUID messageId, boolean exists) {
    if (!exists) {
      return;
    }
    ticketDatabaseTable.findTicket(ticketId).thenAccept(ticket ->
      deleteTicketMessage(ticket, messageId));
  }

  private void deleteTicketMessage(Ticket ticket, UUID messageId) {
    if (!ticket.messages().contains(messageId) || ticket.messages().indexOf(messageId) == 0) {
      return;
    }
    ticketMessageDatabaseTable.deleteTicketMessage(messageId);
    ticketDatabaseTable.removeTicketMessage(ticket.id(), messageId);
  }

  @RequestMapping(path = "/team/ticket/delete/", method = RequestMethod.POST)
  public void deleteTicket(
    HttpServletRequest request, @RequestBody Map<String, Object> input
  ) {
    if (!isAuthenticatedTeamMember(request)) {
      return;
    }
    var ticketId = UUID.fromString((String) input.get("ticket"));
    ticketDatabaseTable.ticketExists(ticketId).thenAccept(exists ->
      deleteTicket(ticketId, exists));
  }

  private void deleteTicket(UUID ticketId, boolean exists) {
    if (!exists) {
      return;
    }
    ticketDatabaseTable.findTicket(ticketId).thenAccept(this::deleteTicket);
  }

  private void deleteTicket(Ticket ticket) {
    ticketDatabaseTable.deleteTicket(ticket.id());
    for (var message : ticket.messages()) {
      ticketMessageDatabaseTable.deleteTicketMessage(message);
    }
  }
}
