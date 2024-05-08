package net.taskwolf.team.access.email;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.iterator.AsyncIterator;
import net.taskwolf.core.mail.TaskwolfMail;
import net.taskwolf.core.mail.TaskwolfMailMessage;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.mail.SupportMail;
import net.taskwolf.team.mail.SupportMailDatabaseTable;
import net.taskwolf.team.mail.SupportMailMessage;
import net.taskwolf.team.mail.SupportMailMessageDatabaseTable;
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
public final class TeamEmailInformationController extends TeamController {
  private final TaskwolfMail supportMail;
  private final SupportMailDatabaseTable supportMailDatabaseTable;
  private final SupportMailMessageDatabaseTable supportMailMessageDatabaseTable;

  private TeamEmailInformationController(
    Key secretKey, UserDatabaseTable userDatabaseTable,
    TeamDatabaseTable teamDatabaseTable, GroupDatabaseTable groupDatabaseTable,
    @Qualifier("supportMail") TaskwolfMail supportMail,
    SupportMailDatabaseTable supportMailDatabaseTable,
    SupportMailMessageDatabaseTable supportMailMessageDatabaseTable
  ) {
    super(secretKey, userDatabaseTable, teamDatabaseTable, groupDatabaseTable);
    this.supportMail = supportMail;
    this.supportMailDatabaseTable = supportMailDatabaseTable;
    this.supportMailMessageDatabaseTable = supportMailMessageDatabaseTable;
  }

  @RequestMapping(path = "/team/mails/find/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findMails(
    HttpServletRequest request
  ) {
    if (!isAuthenticatedTeamMember(request)) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    findOpenMails().thenAccept(mails -> futureResponse.complete(assemblyMails(mails)));
    return futureResponse;
  }

  @RequestMapping(path = "/team/mail/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findMail(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    if (!isAuthenticatedTeamMember(request)) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    var mailId = body.getUUID("mail");
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    supportMailDatabaseTable.mailExists(mailId).thenAccept(exists ->
      findMail(mailId, exists).thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> findMail(
    UUID mailId, boolean exists
  ) {
    if (!exists) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    supportMailDatabaseTable.findMail(mailId).thenAccept(mail ->
      findSupportMailMessages(mail).thenAccept(messages ->
        futureResponse.complete(assemblyMail(mail, messages))));
    return futureResponse;
  }

  private Map<String, Object> assemblyMail(
    SupportMail mail, List<SupportMailMessage> messages
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", mail.requestMessage());
    information.put("title", mail.title());
    information.put("sender", mail.sender());
    messages.sort(Comparator.comparing(SupportMailMessage::time));
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

  private Map<String, Object> assemblyMails(
    Map<SupportMail, List<SupportMailMessage>> mails
  ) {
    var information = Lists.<Map<String, Object>>newArrayList();
    for (var entry : mails.entrySet()) {
      information.add(assemblyMail(entry.getKey(), entry.getValue()));
    }
    return Map.of("mails", information);
  }

  public CompletableFuture<Map<SupportMail, List<SupportMailMessage>>> findOpenMails() {
    var futureResponse = new CompletableFuture<Map<SupportMail, List<SupportMailMessage>>>();
    supportMail.inboxAndFlush().thenAccept(inbox -> saveInbox(new ArrayList<>(inbox))
      .thenAccept(value -> findAllSupportMails().thenAccept(futureResponse::complete)));
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
      supportMailDatabaseTable.findMailsBySender(sender).thenAccept(mails ->
        saveSupportMail(incomingMessage, incomingMessageId, mails.stream().filter(mail ->
            incomingMessage.title().contains(mail.title())).findFirst(), inbox,
          futureResponse)));
  }

  private CompletableFuture<UUID> saveSupportMailMessage(
    TaskwolfMailMessage message
  ) {
    var futureResponse = new CompletableFuture<UUID>();
    var sender = ((InternetAddress) message.sender()).getAddress();
    supportMailMessageDatabaseTable.generateAvailableMessageId().thenAccept(id ->
      supportMailMessageDatabaseTable.insertMailMessage(SupportMailMessage.create(
        id, sender, message.title(), message.body(), message.receiveDate().getTime()))
        .thenAccept(value -> futureResponse.complete(id)));
    return futureResponse;
  }

  private void saveSupportMail(
    TaskwolfMailMessage incomingMessage, UUID incomingMessageId,
    Optional<SupportMail> repliedMail, List<TaskwolfMailMessage> inbox,
    Runnable futureResponse
  ) {
    if (repliedMail.isPresent()) {
      var parentMail = repliedMail.get();
      parentMail.addConversationMessage(incomingMessageId);
      parentMail.disableExpirationTime();
      supportMailDatabaseTable.updateMail(parentMail).thenAccept(value ->
        saveNextSupportMail(inbox, futureResponse));
      return;
    }
    supportMailDatabaseTable.insertMail(incomingMessageId,
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

  private CompletableFuture<Map<SupportMail, List<SupportMailMessage>>> findAllSupportMails() {
    var futureResponse = new CompletableFuture<Map<SupportMail, List<SupportMailMessage>>>();
    supportMailDatabaseTable.findAllMails().thenApply(this::sortOutExpiredMails)
      .thenAccept(mails -> AsyncIterator.execute(mails, mail -> findSupportMailMessages(mail)
          .thenApply(messages -> new AbstractMap.SimpleEntry<>(mail, messages)))
        .thenAccept(entries -> futureResponse.complete(entries.stream()
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))));
    return futureResponse;
  }

  private List<SupportMail> sortOutExpiredMails(List<SupportMail> mails) {
    var sortedOut = Lists.<SupportMail>newArrayList();
    var currentTime = System.currentTimeMillis();
    for (var mail : mails) {
      if (mail.expirationTime() == -1) {
        continue;
      }
      sortedOut.add(mail);
      if (currentTime >= mail.expirationTime()) {
        deleteMail(mail);
      }
    }
    mails.removeAll(sortedOut);
    return mails;
  }

  private void deleteMail(SupportMail mail) {
    supportMailDatabaseTable.deleteMail(mail.requestMessage());
    supportMailMessageDatabaseTable.deleteMailMessage(mail.requestMessage());
    for (var message : mail.conversationMessages()) {
      supportMailMessageDatabaseTable.deleteMailMessage(message);
    }
  }

  private CompletableFuture<List<SupportMailMessage>> findSupportMailMessages(
    SupportMail mail
  ) {
    var futureResponse = new CompletableFuture<List<SupportMailMessage>>();
    AsyncIterator.execute(Stream.concat(mail.conversationMessages().stream(),
          Stream.of(mail.requestMessage())).collect(Collectors.toList()),
        supportMailMessageDatabaseTable::findMailMessage)
      .thenAccept(futureResponse::complete);
    return futureResponse;
  }

  private String formatTime(long time) {
    var calendar = Calendar.getInstance();
    calendar.setTimeInMillis(time);
    return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(calendar.getTime());
  }
}
