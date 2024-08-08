package net.taskwolf.team.access.verification;

import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.team.member.TeamDatabaseTable;
import net.taskwolf.team.member.TeamMember;
import net.taskwolf.team.member.TeamVerificationDatabaseTable;
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

  private VerificationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    TeamVerificationDatabaseTable teamVerificationDatabaseTable
  ) {
    this.secretKey = secretKey;
    this.teamDatabaseTable = teamDatabaseTable;
    this.teamVerificationDatabaseTable = teamVerificationDatabaseTable;
  }

  @RequestMapping(path = "/verification/complete/", method = RequestMethod.POST)
  public void completeVerification(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
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
    var body = TaskwolfRequestBody.of(payload, response);
    var verification = Verification.create(teamDatabaseTable, secretKey,
      body.getString("email"), hashPassword(body.getString("password")));
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    verification.isAuthenticated().thenAccept(isAuthenticated ->
      checkAuthorization(response, verification, futureResponse, isAuthenticated));
    return futureResponse;
  }

  private void checkAuthorization(
    HttpServletResponse servletResponse, Verification verification,
    CompletableFuture<Map<String, Object>> futureResponse, boolean isAuthenticated
  ) {
    if (!isAuthenticated) {
      servletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      futureResponse.complete(Maps.newHashMap());
      return;
    }
    teamDatabaseTable.findTeamMember(verification.email())
      .thenAccept(member -> futureResponse.complete(Map.of("apiKey",
        verification.generateApiKey(member.id()))));
  }

  @RequestMapping(path = "/verification/isValid/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> isValid(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
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
