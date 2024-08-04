package net.taskwolf.team.access.verification;

import io.jsonwebtoken.Jwts;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.taskwolf.team.member.TeamDatabaseTable;

import java.security.Key;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class TeamVerification {
  private final TeamDatabaseTable teamDatabaseTable;
  private final Key secret;
  @Getter
  private final String email;
  private final String passwordHash;

  public CompletableFuture<Boolean> isAuthenticated() {
    var futureResponse = new CompletableFuture<Boolean>();
    if (email == null || passwordHash == null || email.equals("") ||
      passwordHash.equals("")
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
      futureResponse.complete(member.passwordHash().equals(passwordHash)));
    return futureResponse;
  }

  private static final long EXPIRATION_TIME = 1000L * 60 * 60;

  public String generateApiKey(UUID memberId) {
    var expiration = new Date(System.currentTimeMillis() + EXPIRATION_TIME);
    return Jwts.builder()
      .setExpiration(expiration)
      .claim("id", memberId.toString())
      .signWith(secret)
      .compact();
  }
}
