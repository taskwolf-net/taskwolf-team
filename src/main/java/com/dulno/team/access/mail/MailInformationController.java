package com.dulno.team.access.mail;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.mail.OutgoingMail;
import com.dulno.core.mail.OutgoingMailDatabaseTable;
import com.dulno.core.user.UserDatabaseTable;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class MailInformationController extends TeamController {
  private final OutgoingMailDatabaseTable mailDatabaseTable;
  private final UserDatabaseTable userDatabaseTable;

  private MailInformationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    OutgoingMailDatabaseTable mailDatabaseTable,
    UserDatabaseTable userDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.mailDatabaseTable = mailDatabaseTable;
    this.userDatabaseTable = userDatabaseTable;
  }

  @RequestMapping(path = "/mails/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findUserMails(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var userId = body.getUUID("user");
    return memberHasPermission(findMemberId(request), "mails.find")
      .thenCompose(hasPermission -> userDatabaseTable.userExists(userId)
        .thenCompose(exists -> findUserMails(userId, hasPermission, exists)));
  }

  private CompletableFuture<Map<String, Object>> findUserMails(
    UUID userId, boolean hasPermission, boolean userExists
  ) {
    if (!hasPermission || !userExists) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return userDatabaseTable.findUser(userId)
      .thenCompose(user -> mailDatabaseTable.findOutgoingMailsByReceiver(user.email())
        .thenApply(mails -> Map.of("mails", mails.stream()
          .sorted(Comparator.comparingLong(OutgoingMail::time).reversed())
          .map(this::assemblyMailInformation).toList())));
  }

  private Map<String, Object> assemblyMailInformation(OutgoingMail mail) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", mail.id());
    information.put("sender", mail.sender());
    information.put("time", mail.time());
    information.put("title", mail.title());
    return information;
  }
}
