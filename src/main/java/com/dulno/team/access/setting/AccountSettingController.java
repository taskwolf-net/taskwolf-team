package com.dulno.team.access.setting;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.environment.DulnoEnvironment;
import com.dulno.core.hashing.Hashing;
import com.dulno.core.locale.Translation;
import com.dulno.core.mail.Mail;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.dulno.team.member.TeamEmailChangeDatabaseTable;
import com.dulno.team.member.TeamMember;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class AccountSettingController extends TeamController {
  private final TeamEmailChangeDatabaseTable teamEmailChangeDatabaseTable;
  private final Translation translation;
  private final Mail changeMail;
  private final Hashing hashing;
  private final DulnoEnvironment environment;

  private AccountSettingController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    TeamEmailChangeDatabaseTable teamEmailChangeDatabaseTable,
    Translation translation, @Qualifier("changeMail") Mail changeMail,
    Hashing hashing, DulnoEnvironment environment
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.teamEmailChangeDatabaseTable = teamEmailChangeDatabaseTable;
    this.translation = translation;
    this.changeMail = changeMail;
    this.hashing = hashing;
    this.environment = environment;
  }

  @RequestMapping(path = "/settings/account/password/change/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> changePassword(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    findMember(request).thenAccept(member -> futureResponse.complete(
      changePassword(member, body.getString("currentPassword"),
        body.getString("newPassword"))));
    return futureResponse;
  }

  private Map<String, Object> changePassword(
    TeamMember member, String currentPassword, String newPassword
  ) {
    if (!hashing.matches(currentPassword, member.passwordHash())) {
      return Map.of("success", false);
    }
    teamDatabaseTable().changeTeamMemberPassword(member.id(),
      hashing.hash(newPassword));
    return Map.of("success", true);
  }

  @RequestMapping(path = "/settings/account/email/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findEmail(
    HttpServletRequest request
  ) {
    return findMember(request).thenApply(member -> Map.of("email", member.email()));
  }

  @RequestMapping(path = "/settings/account/email/change/request/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> requestEmailChange(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    var newEmail = body.getString("newEmail");
    findMember(request).thenAccept(member -> teamDatabaseTable().teamMemberExists(newEmail)
      .thenAccept(exists -> requestEmailChange(member, body.getString("password"),
        newEmail, exists).thenAccept(futureResponse::complete)));
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> requestEmailChange(
    TeamMember member, String password, String newEmail, boolean accountExists
  ) {
    if (!hashing.matches(password, member.passwordHash())) {
      return CompletableFuture.completedFuture(Map.of("success", false, "errorCode", 1000));
    }
    if (accountExists) {
      return CompletableFuture.completedFuture(Map.of("success", false, "errorCode", 1001));
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    teamEmailChangeDatabaseTable.changeExists(member.id()).thenAccept(requestExists ->
      futureResponse.complete(requestEmailChange(member, newEmail, requestExists)));
    return futureResponse;
  }

  private static final String EMAIL_CHANGE_URL = "https://panel.%s/email/change/complete/%s/%s/";

  private Map<String, Object> requestEmailChange(
    TeamMember member, String newEmail, boolean requestExists
  ) {
    var token = UUID.randomUUID().toString();
    if (requestExists) {
      teamEmailChangeDatabaseTable.updateChange(member.id(), newEmail, token);
    } else {
      teamEmailChangeDatabaseTable.insertChange(member.id(), newEmail, token);
    }
    var title = translation.translate(member.language(), "email.change.email.title");
    var body = String.format(
      translation.translate(member.language(), "email.change.email.body"),
      String.format(EMAIL_CHANGE_URL, environment.domain(),
        member.id().toString(), token));
    changeMail.send(newEmail, member.language(), title, body);
    return Map.of("success", true);
  }

  @RequestMapping(path = "/settings/account/email/change/complete/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> completeEmailChange(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    var memberId = body.getUUID("member");
    teamEmailChangeDatabaseTable.changeExists(memberId).thenAccept(exists ->
      completeEmailChange(memberId, body.getString("token"), exists)
        .thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> completeEmailChange(
    UUID memberId, String token, boolean exists
  ) {
    if (!exists) {
      return CompletableFuture.completedFuture(Map.of("success", false, "errorCode", 1000));
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    teamEmailChangeDatabaseTable.findChange(memberId).thenAccept(changeParameters ->
      completeEmailChange(memberId, token, changeParameters)
        .thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> completeEmailChange(
    UUID memberId, String token, Map.Entry<String, String> changeParameters
  ) {
    if (!token.equals(changeParameters.getValue())) {
      return CompletableFuture.completedFuture(Map.of("success", false, "errorCode", 1001));
    }
    teamEmailChangeDatabaseTable.deleteChange(memberId);
    teamDatabaseTable().changeTeamMemberEmail(memberId, changeParameters.getKey());
    return CompletableFuture.completedFuture(Map.of("success", true));
  }
}
