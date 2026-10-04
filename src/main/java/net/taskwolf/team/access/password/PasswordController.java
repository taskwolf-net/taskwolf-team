package net.taskwolf.team.access.password;

import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.environment.TaskwolfEnvironment;
import net.taskwolf.core.hashing.Hashing;
import net.taskwolf.core.locale.Translation;
import net.taskwolf.core.mail.Mail;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import net.taskwolf.team.member.TeamMember;
import net.taskwolf.team.member.TeamPasswordResetDatabaseTable;
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
public final class PasswordController extends TeamController {
  private final Mail changeMail;
  private final TeamPasswordResetDatabaseTable teamPasswordResetDatabaseTable;
  private final Translation translation;
  private final Hashing hashing;
  private final TaskwolfEnvironment environment;

  private PasswordController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    @Qualifier("changeMail") Mail changeMail,
    TeamPasswordResetDatabaseTable teamPasswordResetDatabaseTable,
    Translation translation, Hashing hashing, TaskwolfEnvironment environment
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.changeMail = changeMail;
    this.teamPasswordResetDatabaseTable = teamPasswordResetDatabaseTable;
    this.translation = translation;
    this.hashing = hashing;
    this.environment = environment;
  }

  @RequestMapping(path = "/password/reset/request/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> requestPasswordReset(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    var email = body.getString("email");
    teamDatabaseTable().teamMemberExists(email).thenAccept(exists ->
      requestPasswordReset(email, exists).thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> requestPasswordReset(
    String email, boolean exists
  ) {
    if (!exists) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "errorCode", 1000));
    }
    return teamDatabaseTable().findTeamMember(email)
      .thenCompose(member -> requestPasswordReset(member,
        "password.reset.email.title", "password.reset.email.body"));
  }

  public CompletableFuture<Map<String, Object>> requestPasswordReset(
    TeamMember member, String emailTitle, String emailBody
  ) {
    return teamPasswordResetDatabaseTable.resetTokenExists(member.id())
      .thenCompose(requestExists -> requestPasswordReset(member, emailTitle,
        emailBody, requestExists));
  }

  private CompletableFuture<Map<String, Object>> requestPasswordReset(
    TeamMember member, String emailTitle, String emailBody, boolean requestExists
  ) {
    if (requestExists) {
      return teamPasswordResetDatabaseTable.findResetToken(member.id())
        .thenApply(token -> sendPasswordResetMail(member, token, emailTitle,
          emailBody));
    }
    var token = UUID.randomUUID().toString();
    teamPasswordResetDatabaseTable.insertResetToken(member.id(), token);
    return CompletableFuture.completedFuture(sendPasswordResetMail(member, token,
      emailTitle, emailBody));
  }

  private static final String PASSWORD_RESET_URL = "https://panel.%s/password/reset/complete/%s/%s/";

  private Map<String, Object> sendPasswordResetMail(
    TeamMember member, String token, String emailTitle, String emailBody
  ) {
    var title = translation.translate(member.language(), emailTitle);
    var body = String.format(translation.translate(member.language(), emailBody),
      member.name(), String.format(PASSWORD_RESET_URL, environment.domain(),
        member.id().toString(), token));
    changeMail.send(member.email(), member.language(), title, body);
    return Map.of("success", true);
  }

  @RequestMapping(path = "/password/reset/complete/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> completePasswordReset(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    var memberId = body.getUUID("member");
    teamPasswordResetDatabaseTable.resetTokenExists(memberId).thenAccept(exists ->
      completePasswordReset(memberId, body.getString("token"), exists,
        body.getString("newPassword")).thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> completePasswordReset(
    UUID memberId, String token, boolean exists, String newPassword
  ) {
    if (!exists) {
      return CompletableFuture.completedFuture(Map.of("success", false, "errorCode", 1000));
    }
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    teamPasswordResetDatabaseTable.findResetToken(memberId).thenAccept(actualToken ->
      completePasswordReset(memberId, token, actualToken, newPassword)
        .thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Map<String, Object>> completePasswordReset(
    UUID memberId, String token, String actualToken, String newPassword
  ) {
    if (!token.equals(actualToken)) {
      return CompletableFuture.completedFuture(Map.of("success", false, "errorCode", 1001));
    }
    teamPasswordResetDatabaseTable.deleteResetToken(memberId);
    teamDatabaseTable().changeTeamMemberPassword(memberId, hashing.hash(newPassword));
    return CompletableFuture.completedFuture(Map.of("success", true));
  }
}

