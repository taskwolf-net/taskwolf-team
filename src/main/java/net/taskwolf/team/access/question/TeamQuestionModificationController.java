package net.taskwolf.team.access.question;

import com.google.common.collect.Lists;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.mail.TaskwolfMail;
import net.taskwolf.core.question.Question;
import net.taskwolf.core.question.QuestionDatabaseTable;
import net.taskwolf.core.question.QuestionMessageDatabaseTable;
import net.taskwolf.core.user.User;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import net.taskwolf.team.member.TeamWorkDatabaseTable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.*;

@RestController
public final class TeamQuestionModificationController extends TeamController {
  private final TaskwolfMail supportMail;
  private final QuestionDatabaseTable questionDatabaseTable;
  private final QuestionMessageDatabaseTable questionMessageDatabaseTable;
  private final TeamWorkDatabaseTable teamWorkDatabaseTable;

  private TeamQuestionModificationController(
    Key secretKey, UserDatabaseTable userDatabaseTable,
    TeamDatabaseTable teamDatabaseTable, GroupDatabaseTable groupDatabaseTable,
    @Qualifier("supportMail") TaskwolfMail supportMail,
    QuestionDatabaseTable questionDatabaseTable,
    QuestionMessageDatabaseTable questionMessageDatabaseTable,
    TeamWorkDatabaseTable teamWorkDatabaseTable
  ) {
    super(secretKey, userDatabaseTable, teamDatabaseTable, groupDatabaseTable);
    this.supportMail = supportMail;
    this.questionDatabaseTable = questionDatabaseTable;
    this.questionMessageDatabaseTable = questionMessageDatabaseTable;
    this.teamWorkDatabaseTable = teamWorkDatabaseTable;
  }

  @RequestMapping(path = "/team/question/delete/", method = RequestMethod.POST)
  public void deleteQuestion(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    if (!isAuthenticatedTeamMember(request)) {
      return;
    }
    var questionId = body.getUUID("question");
    questionDatabaseTable.questionExists(questionId).thenAccept(exists ->
      deleteQuestion(questionId, exists));
  }

  private void deleteQuestion(UUID questionId, boolean exists) {
    if (!exists) {
      return;
    }
    questionDatabaseTable.findQuestion(questionId).thenAccept(this::deleteQuestion);
  }

  private void deleteQuestion(Question question) {
    questionDatabaseTable.deleteQuestion(question.requestMessage());
    questionMessageDatabaseTable.deleteQuestionMessage(question.requestMessage());
    for (var message : question.conversationMessages()) {
      questionMessageDatabaseTable.deleteQuestionMessage(message);
    }
  }

  @RequestMapping(path = "/team/question/answer/", method = RequestMethod.POST)
  public void answerQuestion(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    if (!isAuthenticatedTeamMember(request)) {
      return;
    }
    var questionId = body.getUUID("question");
    findUser(request).thenAccept(teamMember ->
      questionDatabaseTable.questionExists(questionId).thenAccept(exists ->
        answerQuestion(teamMember, questionId, body.getString("message"), exists)));
  }

  private void answerQuestion(
    User teamMember, UUID questionId, String message, boolean exists
  ) {
    if (!exists) {
      return;
    }
    questionDatabaseTable.findQuestion(questionId).thenAccept(question ->
      questionMessageDatabaseTable.generateAvailableMessageId().thenAccept(id ->
        answerQuestion(teamMember, question, message, id)));
  }

  private void answerQuestion(
    User teamMember, Question question, String message, UUID messageId
  ) {
    questionMessageDatabaseTable.insertQuestionMessage(messageId, "Taskwolf",
      message, System.currentTimeMillis());
    question.addConversationMessage(messageId);
    question.resetExpirationTime();
    questionDatabaseTable.updateQuestion(question);
    supportMail.send(question.sender(), "Re: " + question.title(), message);
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
