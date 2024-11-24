package com.dulno.team.access;

import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.dulno.team.member.TeamMember;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.security.Key;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter(AccessLevel.PROTECTED)
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamController {
  private final Key secretKey;
  private final TeamDatabaseTable teamDatabaseTable;
  private final GroupDatabaseTable groupDatabaseTable;

  /**
   * Is used to find the id of the member that send the request
   * @param request The request
   * @return The id of the member
   */
  protected UUID findMemberId(HttpServletRequest request) {
    return findMemberId(findApiKey(request));
  }

  /**
   * Is used to find the id of a member inside an api key
   * @param apiKey The api key
   * @return The id of the member
   */
  protected UUID findMemberId(String apiKey) {
    return UUID.fromString(Jwts.parser().setSigningKey(secretKey).build()
      .parseClaimsJws(apiKey).getPayload().get("id", String.class));
  }

  /**
   * Is used to find the member that send the request
   * @param request The request
   * @return A future that contains the member
   */
  protected CompletableFuture<TeamMember> findMember(HttpServletRequest request) {
    var apiKey = request.getHeader("Authorization").replace("Bearer ", "");
    return teamDatabaseTable.findTeamMember(findMemberId(apiKey));
  }

  /**
   * Is used to find the id of the session that send the request
   * @param request The request
   * @return The id of the session
   */
  protected UUID findSessionId(HttpServletRequest request) {
    return findSessionId(findApiKey(request));
  }

  /**
   * Is used to find the id of a session inside an api key
   * @param apiKey The api key
   * @return The id of the session
   */
  protected UUID findSessionId(String apiKey) {
    return UUID.fromString(Jwts.parser().setSigningKey(secretKey).build()
      .parseClaimsJws(apiKey).getPayload().get("session", String.class));
  }

  /**
   * Is used to find the api key that is sent via a request
   * @param request The request
   * @return The api key
   */
  protected String findApiKey(HttpServletRequest request) {
    return request.getHeader("Authorization").replace("Bearer ", "");
  }

  /**
   * Checks whether an api key is valid
   * @param apiKey The api key
   * @return Is true if api key is valid, otherwise false
   */
  protected boolean isValidApiKey(String apiKey) {
    try {
      Jwts.parser()
        .setSigningKey(secretKey())
        .build()
        .parseClaimsJws(apiKey);
      return true;
    } catch (Exception exception) {
      return false;
    }
  }

  /**
   * Checks whether a team member has a certain permission
   * @param memberId The member
   * @param permission The permission to be checked
   * @return Is true when member has permission, otherwise false
   */
  protected CompletableFuture<Boolean> memberHasPermission(
    UUID memberId, String permission
  ) {
    var completableFuture = new CompletableFuture<Boolean>();
    teamDatabaseTable.findTeamMember(memberId).thenAccept(member ->
      memberHasPermission(member, permission).thenAccept(completableFuture::complete));
    return completableFuture;
  }

  private CompletableFuture<Boolean> memberHasPermission(
    TeamMember member, String permission
  ) {
    if (member.permissions().contains(permission) || member.permissions().contains("*")) {
      return CompletableFuture.completedFuture(true);
    }
    return groupDatabaseTable.findGroup(member.group()).thenApply(group ->
      group.permissions().contains(permission) || group.permissions().contains("*"));
  }
}
