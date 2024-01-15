package net.taskwolf.team.access.email;

import com.google.common.collect.Lists;
import jakarta.servlet.http.HttpServletRequest;
import net.taskwolf.core.mail.TaskwolfMail;
import net.taskwolf.core.user.User;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.mail.SupportMail;
import net.taskwolf.team.mail.SupportMailDatabaseTable;
import net.taskwolf.team.mail.SupportMailMessageDatabaseTable;
import net.taskwolf.team.member.TeamWorkDatabaseTable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.*;

@RestController
public final class TeamEmailModificationController extends TeamController {
  private final TaskwolfMail supportMail;
  private final SupportMailDatabaseTable supportMailDatabaseTable;
  private final SupportMailMessageDatabaseTable supportMailMessageDatabaseTable;
  private final TeamWorkDatabaseTable teamWorkDatabaseTable;

  private TeamEmailModificationController(
    Key secretKey, UserDatabaseTable userDatabaseTable,
    @Qualifier("supportMail") TaskwolfMail supportMail,
    SupportMailDatabaseTable supportMailDatabaseTable,
    SupportMailMessageDatabaseTable supportMailMessageDatabaseTable,
     TeamWorkDatabaseTable teamWorkDatabaseTable
  ) {
    super(secretKey, userDatabaseTable);
    this.supportMail = supportMail;
    this.supportMailDatabaseTable = supportMailDatabaseTable;
    this.supportMailMessageDatabaseTable = supportMailMessageDatabaseTable;
    this.teamWorkDatabaseTable = teamWorkDatabaseTable;
  }

  @RequestMapping(path = "/team/mail/delete/", method = RequestMethod.POST)
  public void deleteMail(
    HttpServletRequest request, @RequestBody Map<String, Object> input
  ) {
    if (!isAuthenticatedTeamMember(request)) {
      return;
    }
    var mailId = UUID.fromString((String) input.get("mail"));
    supportMailDatabaseTable.mailExists(mailId).thenAccept(exists ->
      deleteMail(mailId, exists));
  }

  private void deleteMail(UUID mailId, boolean exists) {
    if (!exists) {
      return;
    }
    supportMailDatabaseTable.findMail(mailId).thenAccept(this::deleteMail);
  }

  private void deleteMail(SupportMail mail) {
    supportMailDatabaseTable.deleteMail(mail.requestMessage());
    supportMailMessageDatabaseTable.deleteMailMessage(mail.requestMessage());
    for (var message : mail.conversationMessages()) {
      supportMailMessageDatabaseTable.deleteMailMessage(message);
    }
  }

  @RequestMapping(path = "/team/mail/answer/", method = RequestMethod.POST)
  public void answerMail(
    HttpServletRequest request, @RequestBody Map<String, Object> input
  ) {
    if (!isAuthenticatedTeamMember(request)) {
      return;
    }
    var mailId = UUID.fromString((String) input.get("mail"));
    var message = (String) input.get("message");
    findUser(request).thenAccept(teamMember -> supportMailDatabaseTable.mailExists(mailId)
      .thenAccept(exists -> answerMail(teamMember, mailId, message, exists)));
  }

  private void answerMail(
    User teamMember, UUID mailId, String message, boolean exists
  ) {
    if (!exists) {
      return;
    }
    supportMailDatabaseTable.findMail(mailId).thenAccept(mail ->
      supportMailMessageDatabaseTable.generateAvailableMessageId().thenAccept(id ->
        answerMail(teamMember, mail, message, id)));
  }

  private void answerMail(
    User teamMember, SupportMail mail, String message, UUID messageId
  ) {
    var title = "Re: " + mail.title();
    supportMailMessageDatabaseTable.insertMailMessage(messageId, "Taskwolf",
      title, message, System.currentTimeMillis());
    mail.addConversationMessage(messageId);
    mail.resetExpirationTime();
    supportMailDatabaseTable.updateMail(mail);
    supportMail.send(mail.sender(), title, message, "text/html; charset=utf-8");
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
}
