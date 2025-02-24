package com.dulno.team.access.verification;

import com.dulno.core.hashing.Hashing;
import com.dulno.team.member.TeamDatabaseTable;
import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.security.Key;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
@AllArgsConstructor(staticName = "create")
public final class Verification {
  private final TeamDatabaseTable teamDatabaseTable;
  private final Key panelSecret;
  private final Key refreshSecret;
  private final Hashing hashing;
  @Getter
  private String email;
  private String password;

  public CompletableFuture<Boolean> isAuthenticated() {
    var futureResponse = new CompletableFuture<Boolean>();
    if (email == null || password == null || email.equals("") ||
      password.equals("")
    ) {
      futureResponse.complete(false);
      return futureResponse;
    }
    teamDatabaseTable.teamMemberExists(email).thenAccept(exists ->
      isAuthenticated(exists).thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Boolean> isAuthenticated(boolean exists) {
    var futureResponse = new CompletableFuture<Boolean>();
    if (!exists) {
      futureResponse.complete(false);
      return futureResponse;
    }
    teamDatabaseTable.findTeamMember(email).thenAccept(member ->
      futureResponse.complete(hashing.matches(password, member.passwordHash())));
    return futureResponse;
  }

  private static final long PANEL_EXPIRATION_TIME = 1000L * 60 * 10;

  public String generatePanelApiKey(UUID memberId, UUID sessionId) {
    var expiration = new Date(System.currentTimeMillis() + PANEL_EXPIRATION_TIME);
    return Jwts.builder().expiration(expiration)
      .claim("id", memberId.toString())
      .claim("session", sessionId.toString())
      .signWith(panelSecret)
      .compact();
  }

  private static final long REFRESH_EXPIRATION_TIME = 1000L * 60 * 60 * 24;

  public String generateRefreshToken(UUID memberId, UUID sessionId) {
    var expiration = new Date(System.currentTimeMillis() + REFRESH_EXPIRATION_TIME);
    return Jwts.builder().expiration(expiration)
      .claim("id", memberId.toString())
      .claim("session", sessionId.toString())
      .signWith(refreshSecret)
      .compact();
  }
}
