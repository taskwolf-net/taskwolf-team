package com.dulno.team.access.entity;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.iterator.AsyncIterator;
import com.dulno.core.organization.Organization;
import com.dulno.core.organization.OrganizationDatabaseTable;
import com.dulno.core.ticket.Ticket;
import com.dulno.core.ticket.TicketDatabaseTable;
import com.dulno.core.user.User;
import com.dulno.core.user.UserDatabaseTable;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class UserController extends TeamController {
  private final UserDatabaseTable userDatabaseTable;
  private final OrganizationDatabaseTable organizationDatabaseTable;
  private final TicketDatabaseTable ticketDatabaseTable;

  private UserController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, UserDatabaseTable userDatabaseTable,
    OrganizationDatabaseTable organizationDatabaseTable,
    TicketDatabaseTable ticketDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.userDatabaseTable = userDatabaseTable;
    this.organizationDatabaseTable = organizationDatabaseTable;
    this.ticketDatabaseTable = ticketDatabaseTable;
  }

  @RequestMapping(path = "/user/search/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> searchUser(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var userMail = body.getString("user");
    return memberHasPermission(findMemberId(request), "user.search")
      .thenCompose(hasPermission -> userDatabaseTable.userExists(userMail)
        .thenCompose(exists -> searchUser(userMail, hasPermission, exists)));
  }

  private CompletableFuture<Map<String, Object>> searchUser(
    String userMail, boolean hasPermission, boolean userExists
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    if (!userExists) {
      return CompletableFuture.completedFuture(Map.of("userFound", false));
    }
    return userDatabaseTable.findUser(userMail)
      .thenApply(user -> Map.of("userFound", true, "userId", user.id()));
  }

  @RequestMapping(path = "/user/find/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findUser(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var userId = body.getUUID("user");
    return memberHasPermission(findMemberId(request), "user.find")
      .thenCompose(hasPermission -> userDatabaseTable.userExists(userId)
        .thenCompose(exists -> findUser(userId, hasPermission, exists)));
  }

  private CompletableFuture<Map<String, Object>> findUser(
    UUID userId, boolean hasPermission, boolean userExists
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    if (!userExists) {
      return CompletableFuture.completedFuture(Map.of("found", false));
    }
    return userDatabaseTable.findUser(userId)
      .thenCompose(user -> AsyncIterator.execute(user.organizations(),
          organizationDatabaseTable::findOrganization)
        .thenCompose(organizations -> ticketDatabaseTable.findAllTicketsOfCreator(userId)
          .thenApply(tickets -> assemblyUserInformation(user, organizations, tickets))));
  }

  private Map<String, Object> assemblyUserInformation(
    User user, List<Organization> organizations, List<Ticket> tickets
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("found", true);
    information.put("id", user.id());
    information.put("email", user.email());
    information.put("name", user.name());
    information.put("language", user.language());
    var organizationInformation = Lists.<Map<String, Object>>newArrayList();
    for (var organization : organizations) {
      organizationInformation.add(Map.of("id", organization.id(),
        "name", organization.name(), "role",
        organization.owner().equals(user.id()) ? "OWNER" : "MEMBER"));
    }
    information.put("organizations", organizationInformation);
    var ticketInformation = Lists.<Map<String, Object>>newArrayList();
    for (var ticket : tickets) {
      ticketInformation.add(Map.of("id", ticket.id(),
        "name", ticket.title(), "type", ticket.type(), "status", ticket.status()));
    }
    information.put("tickets", ticketInformation);
    return information;
  }
}