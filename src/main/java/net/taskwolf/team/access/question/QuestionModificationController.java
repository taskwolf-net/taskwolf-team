package net.taskwolf.team.access.question;

import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.mail.Mail;
import net.taskwolf.core.question.Question;
import net.taskwolf.core.question.QuestionDatabaseTable;
import net.taskwolf.core.question.QuestionMessageDatabaseTable;
import net.taskwolf.core.question.QuestionMessageSenderType;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import net.taskwolf.team.member.TeamMember;
import net.taskwolf.team.member.TeamWorkDatabaseTable;
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
public final class QuestionModificationController extends TeamController {
  private final Mail supportMail;
  private final QuestionDatabaseTable questionDatabaseTable;
  private final QuestionMessageDatabaseTable questionMessageDatabaseTable;
  private final TeamWorkDatabaseTable teamWorkDatabaseTable;

  private QuestionModificationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    @Qualifier("supportMail") Mail supportMail,
    QuestionDatabaseTable questionDatabaseTable,
    QuestionMessageDatabaseTable questionMessageDatabaseTable,
    TeamWorkDatabaseTable teamWorkDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.supportMail = supportMail;
    this.questionDatabaseTable = questionDatabaseTable;
    this.questionMessageDatabaseTable = questionMessageDatabaseTable;
    this.teamWorkDatabaseTable = teamWorkDatabaseTable;
  }

  @RequestMapping(path = "/question/close/", method = RequestMethod.POST)
  public void closeQuestion(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var questionId = body.getUUID("question");
    memberHasPermission(findMemberId(request), "question.close")
      .thenAccept(hasPermission -> questionDatabaseTable.questionExists(questionId)
        .thenAccept(exists -> closeQuestion(questionId, hasPermission, exists)));
  }

  private void closeQuestion(
    UUID questionId, boolean hasPermission, boolean exists
  ) {
    if (!hasPermission || !exists) {
      return;
    }
    questionDatabaseTable.updateQuestionStatus(questionId, Question.Status.CLOSED);
  }

  @RequestMapping(path = "/question/answer/", method = RequestMethod.POST)
  public void answerQuestion(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var questionId = body.getUUID("question");
    findMember(request).thenAccept(teamMember ->
      memberHasPermission(teamMember.id(), "question.answer")
        .thenAccept(hasPermission -> questionDatabaseTable.questionExists(questionId)
          .thenAccept(exists -> answerQuestion(teamMember, questionId,
            body.getString("message"), hasPermission, exists))));
  }

  private void answerQuestion(
    TeamMember teamMember, UUID questionId, String message, boolean hasPermission,
    boolean exists
  ) {
    if (!hasPermission || !exists) {
      return;
    }
    questionDatabaseTable.findQuestion(questionId).thenAccept(question ->
      questionMessageDatabaseTable.generateAvailableMessageId().thenAccept(id ->
        answerQuestion(teamMember, question, message, id)));
  }

  private void answerQuestion(
    TeamMember teamMember, Question question, String message, UUID messageId
  ) {
    question.resetExpirationTime();
    questionDatabaseTable.updateQuestion(question);
    supportMail.send(question.sender(), "Re: " + question.title(), message,
      "text/html; charset=utf-8")
      .thenAccept(publicId -> questionMessageDatabaseTable.insertQuestionMessage(
        messageId, publicId, question.id(), teamMember.id().toString(),
        QuestionMessageSenderType.TEAM, message, System.currentTimeMillis()));
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
}
