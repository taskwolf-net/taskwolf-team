package com.dulno.team.access.entity;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.session.Session;
import com.dulno.core.session.SessionDatabaseTable;
import com.dulno.core.session.SessionStatus;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class UserSessionController extends TeamController {
  private final SessionDatabaseTable sessionDatabaseTable;
  private final UserDatabaseTable userDatabaseTable;

  private UserSessionController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    SessionDatabaseTable sessionDatabaseTable, UserDatabaseTable userDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.sessionDatabaseTable = sessionDatabaseTable;
    this.userDatabaseTable = userDatabaseTable;
  }

  @RequestMapping(path = "/user/sessions/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findUserSessions(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var userId = body.getUUID("user");
    return memberHasPermission(findMemberId(request), "sessions.find")
      .thenCompose(hasPermission -> userDatabaseTable.userExists(userId)
        .thenCompose(exists -> findUserSessions(userId, hasPermission, exists)));
  }

  private CompletableFuture<Map<String, Object>> findUserSessions(
    UUID userId, boolean hasPermission, boolean userExists
  ) {
    if (!hasPermission || !userExists) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return sessionDatabaseTable.findSessionsOfUserByStatus(userId,
        SessionStatus.ACTIVE)
      .thenApply(sessions -> Map.of("sessions", sessions.stream()
        .map(this::assemblySessionInformation).toList()));
  }

  private Map<String, Object> assemblySessionInformation(Session session) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", session.id());
    information.put("platform", session.devicePlatform());
    information.put("country", session.country());
    information.put("city", session.city());
    information.put("openTime", session.openTime());
    information.put("lastRefresh", session.lastRefresh());
    information.put("status", session.status());
    return information;
  }

  @RequestMapping(path = "/user/session/close/", method = RequestMethod.POST)
  public void closeUserSession(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var userId = body.getUUID("user");
    var sessionId = body.getUUID("session");
    memberHasPermission(findMemberId(request), "session.close")
      .thenAccept(hasPermission -> userDatabaseTable.userExists(userId)
        .thenAccept(exists -> closeUserSession(userId, sessionId,
          hasPermission, exists)));
  }

  private void closeUserSession(
    UUID userId, UUID sessionId, boolean hasPermission, boolean userExists
  ) {
    if (!hasPermission || !userExists) {
      return;
    }
    sessionDatabaseTable.sessionExists(sessionId)
      .thenAccept(exists -> closeUserSession(sessionId, userId, exists));
  }

  private void closeUserSession(
    UUID sessionId, UUID userId, boolean sessionExists
  ) {
    if (!sessionExists) {
      return;
    }
    sessionDatabaseTable.findSession(sessionId)
      .thenAccept(session -> closeUserSession(session, userId));
  }

  private void closeUserSession(Session session, UUID userId) {
    if (!session.userId().equals(userId)) {
      return;
    }
    sessionDatabaseTable.closeSession(session.id());
  }
}