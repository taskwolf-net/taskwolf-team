package com.dulno.team.access.verification;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.user.User;
import com.dulno.core.user.mfa.MultiFactorAuthFactory;
import com.dulno.team.member.TeamDatabaseTable;
import com.dulno.team.member.TeamMember;
import com.dulno.team.member.TeamVerificationDatabaseTable;
import com.dulno.team.member.mfa.TeamMultiFactorAuthFactory;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class VerificationController {
  private final Key secretKey;
  private final TeamDatabaseTable teamDatabaseTable;
  private final TeamVerificationDatabaseTable teamVerificationDatabaseTable;
  private final TeamMultiFactorAuthFactory multiFactorAuthFactory;

  private VerificationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    TeamVerificationDatabaseTable teamVerificationDatabaseTable,
    TeamMultiFactorAuthFactory multiFactorAuthFactory
  ) {
    this.secretKey = secretKey;
    this.teamDatabaseTable = teamDatabaseTable;
    this.teamVerificationDatabaseTable = teamVerificationDatabaseTable;
    this.multiFactorAuthFactory = multiFactorAuthFactory;
  }

  @RequestMapping(path = "/verification/complete/", method = RequestMethod.POST)
  public void completeVerification(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var memberId = body.getUUID("member");
    teamVerificationDatabaseTable.verificationExists(memberId)
      .thenAccept(exists -> completeVerification(memberId,
        body.getString("password"), body.getString("token"), exists));
  }

  private void completeVerification(
    UUID memberId, String password, String submittedToken, boolean tokenExists
  ) {
    if (!tokenExists) {
      return;
    }
    teamVerificationDatabaseTable.findVerification(memberId)
      .thenAccept(originalToken -> completeVerification(memberId, password,
        submittedToken, originalToken));
  }

  private void completeVerification(
    UUID memberId, String password, String submittedToken, String originalToken
  ) {
    if (!submittedToken.equals(originalToken)) {
      return;
    }
    teamDatabaseTable.findTeamMember(memberId).thenAccept(member ->
      completeVerification(member, password));
  }

  private void completeVerification(TeamMember member, String password) {
    if (!member.passwordHash().isEmpty()) {
      return;
    }
    teamVerificationDatabaseTable.deleteVerification(member.id());
    teamDatabaseTable.changeTeamMemberPassword(member, hashPassword(password));
  }

  @RequestMapping(path = "/verification/login/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> login(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var loginFuture = login(body.getString("email"), body.getString("password"),
      body.getString("multiFactorCode"));
    loginFuture.thenAccept(result -> applyLoginResponseStatus(response, result));
    return loginFuture;
  }

  private void applyLoginResponseStatus(
    HttpServletResponse response, Map<String, Object> result
  ) {
    if ((boolean) result.get("success")) {
      response.setStatus(HttpServletResponse.SC_OK);
      return;
    }
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  }

  private CompletableFuture<Map<String, Object>> login(
    String email, String password, String multiFactorCode
  ) {
    var verification = Verification.create(teamDatabaseTable, secretKey,
      email.replace(" ", ""), hashPassword(password));
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    verification.isAuthenticated().thenAccept(isAuthenticated ->
      checkAuthorization(verification, multiFactorCode, futureResponse,
        isAuthenticated));
    return futureResponse;
  }

  private void checkAuthorization(
    Verification verification, String multiFactorCode,
    CompletableFuture<Map<String, Object>> futureResponse, boolean isAuthenticated
  ) {
    if (!isAuthenticated) {
      futureResponse.complete(Map.of("success", false, "error", 1000));
      return;
    }
    teamDatabaseTable.findTeamMember(verification.email()).thenAccept(member ->
      multiFactorAuthFactory.createAuth(member.id()).verifyCode(multiFactorCode)
        .thenAccept(verified -> checkMultiFactorAuth(member, verification,
          futureResponse, verified)));
  }

  private void checkMultiFactorAuth(
    TeamMember member, Verification verification,
    CompletableFuture<Map<String, Object>> futureResponse,
    boolean multiFactorVerified
  ) {
    if (!multiFactorVerified) {
      futureResponse.complete(Map.of("success", false, "error", 1001));
      return;
    }
    futureResponse.complete(Map.of("success", true,
      "apiKey", verification.generateApiKey(member.id())));
  }

  @RequestMapping(path = "/verification/isValid/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> isValid(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    try {
      var memberId = UUID.fromString(Jwts.parser()
        .setSigningKey(secretKey)
        .build()
        .parseClaimsJws(body.getString("token"))
        .getPayload().get("id", String.class));
      return teamDatabaseTable.teamMemberExists(memberId).thenApply(exists ->
        Map.of("isValid", exists ? "true" : "false"));
    } catch (Exception exception) {
      return CompletableFuture.completedFuture(Map.of("isValid", "false"));
    }
  }

  private String hashPassword(String password) {
    return Hashing.sha256().hashString(password, StandardCharsets.UTF_8)
      .toString();
  }
}
