package net.taskwolf.team.access.question;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.iterator.AsyncIterator;
import net.taskwolf.core.mail.TaskwolfMail;
import net.taskwolf.core.mail.TaskwolfMailMessage;
import net.taskwolf.core.question.Question;
import net.taskwolf.core.question.QuestionDatabaseTable;
import net.taskwolf.core.question.QuestionMessage;
import net.taskwolf.core.question.QuestionMessageDatabaseTable;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
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
import java.util.stream.Stream;

@RestController
public final class TeamQuestionInformationController extends TeamController {
  private final TaskwolfMail supportMail;
  private final QuestionDatabaseTable questionDatabaseTable;
  private final QuestionMessageDatabaseTable questionMessageDatabaseTable;

  private TeamQuestionInformationController(
    Key secretKey, UserDatabaseTable userDatabaseTable,
    TeamDatabaseTable teamDatabaseTable, GroupDatabaseTable groupDatabaseTable,
    @Qualifier("supportMail") TaskwolfMail supportMail,
    QuestionDatabaseTable questionDatabaseTable,
    QuestionMessageDatabaseTable questionMessageDatabaseTable
  ) {
    super(secretKey, userDatabaseTable, teamDatabaseTable, groupDatabaseTable);
    this.supportMail = supportMail;
    this.questionDatabaseTable = questionDatabaseTable;
    this.questionMessageDatabaseTable = questionMessageDatabaseTable;
  }

  @RequestMapping(path = "/team/questions/find/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findQuestions(
    HttpServletRequest request
  ) {
    if (!isAuthenticatedTeamMember(request)) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    findOpenQuestions().thenAccept(questions ->
      futureResponse.complete(assemblyQuestions(questions)));
    return futureResponse;
  }

  @RequestMapping(path = "/team/questions/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findQuestion(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    if (!isAuthenticatedTeamMember(request)) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    var questionId = body.getUUID("question");
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    questionDatabaseTable.questionExists(questionId).thenAccept(exists ->
      findQuestion(questionId, exists).thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> findQuestion(
    UUID questionId, boolean exists
  ) {
    if (!exists) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    questionDatabaseTable.findQuestion(questionId).thenAccept(question ->
      findQuestionMessages(question).thenAccept(messages ->
        futureResponse.complete(assemblyQuestion(question, messages))));
    return futureResponse;
  }

  private Map<String, Object> assemblyQuestion(
    Question question, List<QuestionMessage> messages
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", question.requestMessage());
    information.put("title", question.title());
    information.put("sender", question.sender());
    messages.sort(Comparator.comparing(QuestionMessage::time));
    information.put("date", formatTime(messages.get(messages.size() - 1).time()));
    var messagesInformation = Lists.<Map<String, Object>>newArrayList();
    for (var message : messages) {
      var messageInformation = Maps.<String, Object>newHashMap();
      messageInformation.put("id", message.id());
      messageInformation.put("sender", message.sender());
      messageInformation.put("body", message.body());
      messageInformation.put("teamWritten", message.sender().equalsIgnoreCase("Taskwolf"));
      messageInformation.put("time", formatTime(message.time()));
      messagesInformation.add(messageInformation);
    }
    information.put("messages", messagesInformation);
    return information;
  }

  private Map<String, Object> assemblyQuestions(
    Map<Question, List<QuestionMessage>> questions
  ) {
    var information = Lists.<Map<String, Object>>newArrayList();
    for (var entry : questions.entrySet()) {
      information.add(assemblyQuestion(entry.getKey(), entry.getValue()));
    }
    return Map.of("mails", information);
  }

  public CompletableFuture<Map<Question, List<QuestionMessage>>> findOpenQuestions() {
    var futureResponse = new CompletableFuture<Map<Question, List<QuestionMessage>>>();
    supportMail.inboxAndFlush().thenAccept(inbox -> saveInbox(new ArrayList<>(inbox))
      .thenAccept(value -> findAllSupportQuestions().thenAccept(futureResponse::complete)));
    return futureResponse;
  }

  private CompletableFuture<Void> saveInbox(List<TaskwolfMailMessage> inbox) {
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
    TaskwolfMailMessage incomingMessage, List<TaskwolfMailMessage> inbox,
    Runnable futureResponse
  ) {
    var sender = ((InternetAddress) incomingMessage.sender()).getAddress();
    saveSupportMailMessage(incomingMessage).thenAccept(incomingMessageId ->
      questionDatabaseTable.findQuestionsBySender(sender).thenAccept(mails ->
        saveSupportMail(incomingMessage, incomingMessageId, mails.stream().filter(mail ->
            incomingMessage.title().contains(mail.title())).findFirst(), inbox,
          futureResponse)));
  }

  private CompletableFuture<UUID> saveSupportMailMessage(
    TaskwolfMailMessage message
  ) {
    var futureResponse = new CompletableFuture<UUID>();
    var sender = ((InternetAddress) message.sender()).getAddress();
    questionMessageDatabaseTable.generateAvailableMessageId().thenAccept(id ->
      questionMessageDatabaseTable.insertQuestionMessage(QuestionMessage.create(
        id, sender, message.title(), message.body(), message.receiveDate().getTime()))
        .thenAccept(value -> futureResponse.complete(id)));
    return futureResponse;
  }

  private void saveSupportMail(
    TaskwolfMailMessage incomingMessage, UUID incomingMessageId,
    Optional<Question> repliedMail, List<TaskwolfMailMessage> inbox,
    Runnable futureResponse
  ) {
    if (repliedMail.isPresent()) {
      var parentMail = repliedMail.get();
      parentMail.addConversationMessage(incomingMessageId);
      parentMail.disableExpirationTime();
      questionDatabaseTable.updateQuestion(parentMail).thenAccept(value ->
        saveNextSupportMail(inbox, futureResponse));
      return;
    }
    questionDatabaseTable.insertQuestion(incomingMessageId,
        ((InternetAddress) incomingMessage.sender()).getAddress(),
        incomingMessage.title(), -1, Lists.newArrayList())
      .thenAccept(value -> saveNextSupportMail(inbox, futureResponse));
  }

  private void saveNextSupportMail(
    List<TaskwolfMailMessage> inbox, Runnable futureResponse
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
    questionDatabaseTable.findAllQuestions().thenApply(this::sortOutExpiredQuestions)
      .thenAccept(questions -> AsyncIterator.execute(questions, question -> findQuestionMessages(question)
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
        deleteQuestion(question);
      }
    }
    questions.removeAll(sortedOut);
    return questions;
  }

  private void deleteQuestion(Question question) {
    questionDatabaseTable.deleteQuestion(question.requestMessage());
    questionMessageDatabaseTable.deleteQuestionMessage(question.requestMessage());
    for (var message : question.conversationMessages()) {
      questionMessageDatabaseTable.deleteQuestionMessage(message);
    }
  }

  private CompletableFuture<List<QuestionMessage>> findQuestionMessages(
    Question question
  ) {
    var futureResponse = new CompletableFuture<List<QuestionMessage>>();
    AsyncIterator.execute(Stream.concat(question.conversationMessages().stream(),
          Stream.of(question.requestMessage())).collect(Collectors.toList()),
        questionMessageDatabaseTable::findQuestionMessage)
      .thenAccept(futureResponse::complete);
    return futureResponse;
  }

  private String formatTime(long time) {
    var calendar = Calendar.getInstance();
    calendar.setTimeInMillis(time);
    return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(calendar.getTime());
  }
}
