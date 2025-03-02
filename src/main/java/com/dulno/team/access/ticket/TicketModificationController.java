package com.dulno.team.access.ticket;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.environment.DulnoEnvironment;
import com.dulno.core.locale.Translation;
import com.dulno.core.mail.Mail;
import com.dulno.core.ticket.*;
import com.dulno.core.user.User;
import com.dulno.core.user.UserDatabaseTable;
import com.dulno.core.user.activity.ActivityType;
import com.dulno.core.user.activity.UserActivityDatabaseTable;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.dulno.team.member.TeamMember;
import com.dulno.team.member.TeamWorkDatabaseTable;
import com.google.common.collect.Lists;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.UUID;

@RestController
public final class TicketModificationController extends TeamController {
  private final TicketDatabaseTable ticketDatabaseTable;
  private final TicketMessageDatabaseTable ticketMessageDatabaseTable;
  private final TeamWorkDatabaseTable teamWorkDatabaseTable;
  private final UserDatabaseTable userDatabaseTable;
  private final Translation translation;
  private final Mail supportMail;
  private final UserActivityDatabaseTable activityDatabaseTable;
  private final DulnoEnvironment environment;

  private TicketModificationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, TicketDatabaseTable ticketDatabaseTable,
    TicketMessageDatabaseTable ticketMessageDatabaseTable,
    TeamWorkDatabaseTable teamWorkDatabaseTable,
    UserDatabaseTable userDatabaseTable, Translation translation,
    @Qualifier("supportMail") Mail supportMail,
    UserActivityDatabaseTable activityDatabaseTable,
    DulnoEnvironment environment
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.ticketDatabaseTable = ticketDatabaseTable;
    this.ticketMessageDatabaseTable = ticketMessageDatabaseTable;
    this.teamWorkDatabaseTable = teamWorkDatabaseTable;
    this.userDatabaseTable = userDatabaseTable;
    this.translation = translation;
    this.supportMail = supportMail;
    this.activityDatabaseTable = activityDatabaseTable;
    this.environment = environment;
  }

  @RequestMapping(path = "/ticket/answer/", method = RequestMethod.POST)
  public void answerTicket(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var ticketId = body.getUUID("ticket");
    findMember(request).thenAccept(teamMember ->
      memberHasPermission(teamMember.id(), "ticket.answer")
        .thenAccept(hasPermission -> ticketDatabaseTable.ticketExists(ticketId)
          .thenAccept(exists -> answerTicket(teamMember, ticketId,
            body.getString("message"), hasPermission, exists))));
  }

  private void answerTicket(
    TeamMember teamMember, UUID ticketId, String message, boolean hasPermission,
    boolean exists
  ) {
    if (!hasPermission || !exists) {
      return;
    }
    ticketDatabaseTable.findTicket(ticketId).thenAccept(ticket ->
      ticketMessageDatabaseTable.generateAvailableTicketMessageId()
        .thenAccept(id -> userDatabaseTable.findUser(ticket.creator())
          .thenAccept(creator -> answerTicket(teamMember, ticket,
            message, id, creator))));
  }

  private static final String TICKET_URL_FORMAT = "https://%s/ticket/%s/";

  private void answerTicket(
    TeamMember teamMember, Ticket ticket, String message, UUID messageId,
    User creator
  ) {
    ticket.addMessage(messageId);
    ticket.resetExpirationTime();
    ticket.updateLastMessageSeen(false);
    ticketDatabaseTable.deleteTicket(ticket.id())
      .thenAccept(deleteValue -> ticketDatabaseTable.insertTicket(ticket)
        .thenAccept(insertValue -> ticketMessageDatabaseTable
          .insertTicketMessage(TicketMessage.create(messageId, ticket.id(),
            teamMember.id(), TicketMessageAuthorType.TEAM, message,
            System.currentTimeMillis()))));
    activityDatabaseTable.insertActivity(ticket.creator(),
      "activity.ticket.answer.title", "activity.ticket.answer.description",
      ActivityType.TICKET);
    var title = translation.translate(creator, "ticket.answer.email.title");
    var body = String.format(translation.translate(creator, "ticket.answer.email.body"),
      creator.name(), ticket.title(), String.format(TICKET_URL_FORMAT,
        environment.domain(), ticket.id()));
    supportMail.send(creator, title, body);
    teamWorkDatabaseTable.hasRequestReplies(teamMember.id())
      .thenAccept(exists -> addTeamMemberReply(teamMember, exists));
  }

  private void addTeamMemberReply(TeamMember teamMember, boolean workExists) {
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
    var body = DulnoRequestBody.of(payload, response);
    var ticketId = body.getUUID("ticket");
    memberHasPermission(findMemberId(request), "ticket.message.delete")
      .thenAccept(hasPermission -> ticketDatabaseTable.ticketExists(ticketId)
        .thenAccept(exists -> deleteTicketMessage(ticketId,
          body.getUUID("message"), hasPermission, exists)));
  }

  private void deleteTicketMessage(
    UUID ticketId, UUID messageId, boolean hasPermission, boolean exists
  ) {
    if (!hasPermission || !exists) {
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

  @RequestMapping(path = "/ticket/close/", method = RequestMethod.POST)
  public void closeTicket(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var ticketId = body.getUUID("ticket");
    memberHasPermission(findMemberId(request), "ticket.close")
      .thenAccept(hasPermission -> ticketDatabaseTable.ticketExists(ticketId)
        .thenAccept(exists -> closeTicket(ticketId, hasPermission, exists)));
  }

  private void closeTicket(
    UUID ticketId, boolean hasPermission, boolean exists
  ) {
    if (!hasPermission || !exists) {
      return;
    }
    ticketDatabaseTable.updateTicketStatus(ticketId, Ticket.Status.CLOSED);
  }
}
