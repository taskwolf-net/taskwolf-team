package net.taskwolf.team.access.ticket;

import com.google.common.collect.Lists;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.ticket.Ticket;
import net.taskwolf.core.ticket.TicketDatabaseTable;
import net.taskwolf.core.ticket.TicketMessage;
import net.taskwolf.core.ticket.TicketMessageDatabaseTable;
import net.taskwolf.core.user.User;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.core.user.activity.ActivityType;
import net.taskwolf.core.user.activity.UserActivityDatabaseTable;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import net.taskwolf.team.member.TeamWorkDatabaseTable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.UUID;

@RestController
public final class TeamTicketModificationController extends TeamController {
  private final TicketDatabaseTable ticketDatabaseTable;
  private final TicketMessageDatabaseTable ticketMessageDatabaseTable;
  private final TeamWorkDatabaseTable teamWorkDatabaseTable;
  private final UserActivityDatabaseTable activityDatabaseTable;

  private TeamTicketModificationController(
    Key secretKey, UserDatabaseTable userDatabaseTable,
    TeamDatabaseTable teamDatabaseTable, GroupDatabaseTable groupDatabaseTable,
    TicketDatabaseTable ticketDatabaseTable,
    TicketMessageDatabaseTable ticketMessageDatabaseTable,
    TeamWorkDatabaseTable teamWorkDatabaseTable,
    UserActivityDatabaseTable activityDatabaseTable
  ) {
    super(secretKey, userDatabaseTable, teamDatabaseTable, groupDatabaseTable);
    this.ticketDatabaseTable = ticketDatabaseTable;
    this.ticketMessageDatabaseTable = ticketMessageDatabaseTable;
    this.teamWorkDatabaseTable = teamWorkDatabaseTable;
    this.activityDatabaseTable = activityDatabaseTable;
  }

  @RequestMapping(path = "/ticket/answer/", method = RequestMethod.POST)
  public void answerTicket(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    if (!isAuthenticatedTeamMember(request)) {
      return;
    }
    var ticketId = body.getUUID("ticket");
    findUser(request).thenAccept(teamMember ->
      ticketDatabaseTable.ticketExists(ticketId).thenAccept(exists ->
        answerTicket(teamMember, ticketId, body.getString("message"), exists)));
  }

  private void answerTicket(
    User teamMember, UUID ticketId, String message, boolean exists
  ) {
    if (!exists) {
      return;
    }
    ticketDatabaseTable.findTicket(ticketId).thenAccept(ticket ->
      ticketMessageDatabaseTable.generateAvailableTicketMessageId().thenAccept(id ->
        answerTicket(teamMember, ticket, message, id)));
  }

  private static final UUID TASKWOLF_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private void answerTicket(
    User teamMember, Ticket ticket, String message, UUID messageId
  ) {
    ticket.addMessage(messageId);
    ticket.resetExpirationTime();
    ticketDatabaseTable.updateTicket(ticket);
    ticketMessageDatabaseTable.insertTicketMessage(TicketMessage.create(messageId,
      ticket.id(), TASKWOLF_USER_ID, message, System.currentTimeMillis()));
    activityDatabaseTable.insertActivity(ticket.creator(), "activity.ticket.answer.title",
      "activity.ticket.answer.description", ActivityType.TICKET);
    teamWorkDatabaseTable.hasRequestReplies(teamMember.id())
      .thenAccept(exists -> addTeamMemberReply(teamMember, exists));
  }

  private void addTeamMemberReply(User teamMember, boolean workExists) {
    if (!workExists) {
      teamWorkDatabaseTable.insertRequestReplies(teamMember.id(),
        Lists.newArrayList(System.currentTimeMillis()));
      return;
    }
    teamWorkDatabaseTable.addRequestReply(teamMember.id(),
      System.currentTimeMillis());
  }

  @RequestMapping(path = "/ticket/message/delete/", method = RequestMethod.POST)
  public void deleteTicketMessage(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    if (!isAuthenticatedTeamMember(request)) {
      return;
    }
    var ticketId = body.getUUID("ticket");
    ticketDatabaseTable.ticketExists(ticketId).thenAccept(exists ->
      deleteTicketMessage(ticketId, body.getUUID("message"), exists));
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

  @RequestMapping(path = "/ticket/delete/", method = RequestMethod.POST)
  public void deleteTicket(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    if (!isAuthenticatedTeamMember(request)) {
      return;
    }
    var ticketId = body.getUUID("ticket");
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
