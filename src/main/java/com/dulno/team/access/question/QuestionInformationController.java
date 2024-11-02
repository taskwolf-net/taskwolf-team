package com.dulno.team.access.question;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.iterator.AsyncIterator;
import com.dulno.core.mail.Mail;
import com.dulno.core.mail.MailMessage;
import com.dulno.core.question.*;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.dulno.team.member.TeamMember;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.internet.InternetAddress;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
public final class QuestionInformationController extends TeamController {
  private final Mail supportMail;
  private final QuestionDatabaseTable questionDatabaseTable;
  private final QuestionMessageDatabaseTable questionMessageDatabaseTable;

  private QuestionInformationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    @Qualifier("supportMail") Mail supportMail,
    QuestionDatabaseTable questionDatabaseTable,
    QuestionMessageDatabaseTable questionMessageDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.supportMail = supportMail;
    this.questionDatabaseTable = questionDatabaseTable;
    this.questionMessageDatabaseTable = questionMessageDatabaseTable;
  }

  @RequestMapping(path = "/questions/pending/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> countPendingTickets() {
    return questionDatabaseTable.countPendingQuestions().thenApply(questions ->
      Map.of("questions", questions));
  }

  @RequestMapping(path = "/questions/find/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findQuestions(
    HttpServletRequest request
  ) {
    return memberHasPermission(findMemberId(request), "questions.find")
      .thenCompose(this::findQuestions);
  }

  private CompletableFuture<Map<String, Object>> findQuestions(
    boolean hasPermission
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return findOpenQuestions().thenCompose(this::assemblyQuestions);
  }

  private CompletableFuture<Map<String, Object>> assemblyQuestions(
    Map<Question, List<QuestionMessage>> questions
  ) {
    return AsyncIterator.execute(Lists.newArrayList(questions.entrySet()),
        entry -> questionInformation(entry.getKey(), entry.getValue()))
      .thenApply(information -> Map.of("questions", information));
  }

  @RequestMapping(path = "/question/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findQuestion(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var questionId = body.getUUID("question");
    return memberHasPermission(findMemberId(request), "question.find")
      .thenCompose(hasPermission -> questionDatabaseTable.questionExists(questionId)
        .thenCompose(exists -> findQuestion(questionId, hasPermission, exists)));
  }

  private CompletableFuture<Map<String, Object>> findQuestion(
    UUID questionId, boolean hasPermission, boolean exists
  ) {
    if (!hasPermission || !exists) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return questionDatabaseTable.findQuestion(questionId).thenCompose(question ->
      questionMessageDatabaseTable.findMessagesOfQuestion(question.id())
        .thenCompose(messages -> questionInformation(question, messages)));
  }

  private CompletableFuture<Map<String, Object>> questionInformation(
    Question question, List<QuestionMessage> messages
  ) {
    var sortedMessages = messages.stream()
      .sorted(Comparator.comparing(QuestionMessage::time)).toList();
    return AsyncIterator.execute(messages, this::messageInformation)
      .thenApply(information -> information.stream()
        .sorted(Comparator.comparing(entry -> (long) entry.get("rawTime"))).toList())
      .thenApply(information -> assemblyQuestionInformation(question,
        sortedMessages, information));
  }

  private Map<String, Object> assemblyQuestionInformation(
    Question question, List<QuestionMessage> messages,
    List<Map<String, Object>> messageInformation
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", question.id());
    information.put("title", question.title());
    information.put("status", question.status());
    information.put("sender", question.sender());
    information.put("date", formatTime(messages.get(messages.size() - 1).time()));
    information.put("messages", messageInformation);
    return information;
  }

  private CompletableFuture<Map<String, Object>> messageInformation(
    QuestionMessage message
  ) {
    var teamWritten = message.senderType().isTeam();
    var futureSender = teamWritten ?
      teamDatabaseTable().findTeamMember(UUID.fromString(message.sender()))
        .thenApply(TeamMember::name) :
      CompletableFuture.completedFuture(message.sender());
    return futureSender.thenApply(sender ->
      assemblyMessageInformation(message, sender, teamWritten));
  }

  private Map<String, Object> assemblyMessageInformation(
    QuestionMessage message, String sender, boolean teamWritten
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", message.id());
    information.put("sender", sender);
    information.put("body", message.content());
    information.put("teamWritten", teamWritten);
    information.put("time", formatTime(message.time()));
    information.put("rawTime", message.time());
    return information;
  }

  public CompletableFuture<Map<Question, List<QuestionMessage>>> findOpenQuestions() {
    var futureResponse = new CompletableFuture<Map<Question, List<QuestionMessage>>>();
    supportMail.inboxAndFlush().thenAccept(inbox -> saveInbox(new ArrayList<>(inbox))
      .thenAccept(value -> findAllSupportQuestions().thenAccept(futureResponse::complete)));
    return futureResponse;
  }

  private CompletableFuture<Void> saveInbox(List<MailMessage> inbox) {
    if (inbox.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }
    inbox.sort(Comparator.comparing(message -> message.receiveDate().getTime()));
    var futureResponse = new CompletableFuture<Void>();
    var firstElement = inbox.remove(0);
    saveSupportMail(firstElement, inbox, () -> futureResponse.complete(null));
    return futureResponse;
  }

  private void saveSupportMail(
    MailMessage incomingMessage, List<MailMessage> inbox,
    Runnable futureResponse
  ) {
    questionMessageDatabaseTable.questionMessageExists(incomingMessage.inReplyTo())
      .thenCompose(exists -> findQuestion(incomingMessage.inReplyTo(), exists))
      .thenAccept(question -> saveSupportMail(incomingMessage, question, inbox,
        futureResponse));
  }

  private CompletableFuture<Question> findQuestion(
    String inReplyTo, boolean exists
  ) {
    if (!exists) {
      return CompletableFuture.completedFuture(null);
    }
    return questionMessageDatabaseTable.findQuestionMessage(inReplyTo)
      .thenCompose(message ->
        questionDatabaseTable.findQuestion(message.questionId()));
  }

  private void saveSupportMail(
    MailMessage incomingMessage, Question repliedMail, List<MailMessage> inbox,
    Runnable futureResponse
  ) {
    if (repliedMail != null) {
      repliedMail.disableExpirationTime();
      questionDatabaseTable.updateQuestion(repliedMail)
        .thenCompose(value -> saveSupportMailMessage(repliedMail.id(),
          incomingMessage))
        .thenAccept(value -> saveNextSupportMail(inbox, futureResponse));
      return;
    }
    questionDatabaseTable.generateAvailableQuestionId()
      .thenAccept(id -> questionDatabaseTable.insertQuestion(id,
          ((InternetAddress) incomingMessage.sender()).getAddress(),
          incomingMessage.title(), Question.Status.OPEN.toString(), -1)
        .thenCompose(value -> saveSupportMailMessage(id, incomingMessage))
        .thenAccept(value -> saveNextSupportMail(inbox, futureResponse)));
  }

  private CompletableFuture<Void> saveSupportMailMessage(
    UUID questionId, MailMessage message
  ) {
    var sender = ((InternetAddress) message.sender()).getAddress();
    return questionMessageDatabaseTable.generateAvailableMessageId()
      .thenCompose(id -> questionMessageDatabaseTable.insertQuestionMessage(
        QuestionMessage.create(id, message.messageId(), questionId, sender,
          QuestionMessageSenderType.USER, message.body(),
          message.receiveDate().getTime())));
  }

  private void saveNextSupportMail(
    List<MailMessage> inbox, Runnable futureResponse
  ) {
    if (inbox.isEmpty()) {
      futureResponse.run();
      return;
    }
    var firstElement = inbox.remove(0);
    saveSupportMail(firstElement, inbox, futureResponse);
  }

  private CompletableFuture<Map<Question, List<QuestionMessage>>> findAllSupportQuestions() {
    var futureResponse = new CompletableFuture<Map<Question, List<QuestionMessage>>>();
    questionDatabaseTable.findOpenQuestions().thenApply(this::sortOutExpiredQuestions)
      .thenAccept(questions -> AsyncIterator.execute(questions, question ->
          questionMessageDatabaseTable.findMessagesOfQuestion(question.id())
            .thenApply(messages -> new AbstractMap.SimpleEntry<>(question, messages)))
        .thenAccept(entries -> futureResponse.complete(entries.stream()
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))));
    return futureResponse;
  }

  private List<Question> sortOutExpiredQuestions(List<Question> questions) {
    var sortedOut = Lists.<Question>newArrayList();
    var currentTime = System.currentTimeMillis();
    for (var question : questions) {
      if (question.expirationTime() == -1) {
        continue;
      }
      sortedOut.add(question);
      if (currentTime >= question.expirationTime()) {
        questionDatabaseTable.updateQuestionStatus(question.id(),
          Question.Status.CLOSED);
      }
    }
    questions.removeAll(sortedOut);
    return questions;
  }

  private String formatTime(long time) {
    var calendar = Calendar.getInstance();
    calendar.setTimeInMillis(time);
    return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(calendar.getTime());
  }
}
