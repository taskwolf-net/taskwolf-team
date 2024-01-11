package net.taskwolf.team.access;

import com.datastax.oss.driver.shaded.guava.common.collect.Maps;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import jakarta.servlet.http.HttpServletRequest;
import net.taskwolf.core.iterator.AsyncIterator;
import net.taskwolf.core.mail.TaskwolfMail;
import net.taskwolf.core.mail.TaskwolfMailMessage;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.mail.SupportMail;
import net.taskwolf.team.mail.SupportMailDatabaseTable;
import net.taskwolf.team.mail.SupportMailMessage;
import net.taskwolf.team.mail.SupportMailMessageDatabaseTable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.security.Key;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class TeamEmailController extends TeamController {
  private final TaskwolfMail supportMail;
  private final SupportMailDatabaseTable supportMailDatabaseTable;
  private final SupportMailMessageDatabaseTable supportMailMessageDatabaseTable;

  private TeamEmailController(
    Key secretKey, UserDatabaseTable userDatabaseTable,
    @Qualifier("supportMail") TaskwolfMail supportMail,
    SupportMailDatabaseTable supportMailDatabaseTable,
    SupportMailMessageDatabaseTable supportMailMessageDatabaseTable
  ) {
    super(secretKey, userDatabaseTable);
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
    checkInbox().thenAccept(inbox -> supportMailDatabaseTable.findAllMails()
      .thenAccept(mails -> AsyncIterator.execute(mails, mail -> findMailMessages(mail)
        .thenApply(messages -> new AbstractMap.SimpleEntry(mails, messages)),
        mails.size(), entries -> futureResponse.complete(assemblyMails(
          Map.ofEntries(entries.toArray(new Map.Entry[0])), inbox)))));
    return futureResponse;
  }

  private CompletableFuture<List<SupportMailMessage>> findMailMessages(
    SupportMail mail
  ) {
    var futureResponse = new CompletableFuture<List<SupportMailMessage>>();
    AsyncIterator.execute(mail.conversationMessageIds(),
      supportMailMessageDatabaseTable::findMailMessage,
      mail.conversationMessageIds().size(), futureResponse::complete);
    return futureResponse;
  }

  private Map<String, Object> assemblyMails(
    Map<SupportMail, List<SupportMailMessage>> mails,
    List<TaskwolfMailMessage> newMessages
  ) {
    return null;
  }

  private Map<SupportMail, List<SupportMailMessage>> supplementSupportMailsWithInbox(
    Map<SupportMail, List<SupportMailMessage>> mails,
    List<TaskwolfMailMessage> newMessages
  ) {
    /*var requests = newMessages.stream().filter(message ->
      message.inReplyTo().equals("")).toList();
    var replies = newMessages.stream().filter(message ->
      !message.inReplyTo().equals("")).toList();
    var requestReplies = HashMultimap.<String, String>create();
    for (var reply : replies) {
      if (requests.stream().anyMatch(request -> request.messageId().equals(reply.inReplyTo()))) {
        requestReplies.put(reply.inReplyTo(), reply.messageId());
      } else {
        mails.put(mails.keySet().stream().filter(request -> request.requestMessageId().equals(reply.inReplyTo()))))
      }
    }
    return requestReplies;*/
    return null;
  }

  private CompletableFuture<List<TaskwolfMailMessage>> checkInbox() {
    var futureResponse = new CompletableFuture<List<TaskwolfMailMessage>>();
    supportMail.inboxAndFlush().thenAccept(futureResponse::complete);
    futureResponse.thenAccept(this::saveInbox);
    return futureResponse;
  }

  private void saveInbox(List<TaskwolfMailMessage> messages) {
    var requests = messages.stream().filter(message ->
      message.inReplyTo().equals("")).toList();
    var replies = messages.stream().filter(message ->
      !message.inReplyTo().equals("")).toList();
    var requestReplies = saveReplies(requests, replies);
    saveRequests(requests, requestReplies);
  }

  private Multimap<String, String> saveReplies(
    List<TaskwolfMailMessage> requests, List<TaskwolfMailMessage> replies
  ) {
    var requestReplies = HashMultimap.<String, String>create();
    for (var reply : replies) {
      supportMailMessageDatabaseTable.insertMailMessage(reply.messageId(),
        reply.title(), reply.body());
      if (requests.stream().anyMatch(request -> request.messageId().equals(reply.inReplyTo()))) {
        requestReplies.put(reply.inReplyTo(), reply.messageId());
      } else {
        supportMailDatabaseTable.addMailMessage(reply.inReplyTo(), reply.messageId());
      }
    }
    return requestReplies;
  }

  private void saveRequests(
    List<TaskwolfMailMessage> requests, Multimap<String, String> requestReplies
  ) {
    for (var request : requests) {
      supportMailMessageDatabaseTable.insertMailMessage(request.messageId(),
        request.title(), request.body());
      supportMailDatabaseTable.insertMail(request.messageId(),
        !requestReplies.containsKey(request.messageId()) ? Lists.newArrayList() :
          requestReplies.get(request.messageId()).stream().toList());
    }
  }
}
