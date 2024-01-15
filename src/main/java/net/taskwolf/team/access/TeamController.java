package net.taskwolf.team.access;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.taskwolf.core.access.TaskwolfRestController;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import net.taskwolf.team.member.TeamMember;

import java.security.Key;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
@Accessors(fluent = true)
public class TeamController extends TaskwolfRestController {
  private final TeamDatabaseTable teamDatabaseTable;
  private final GroupDatabaseTable groupDatabaseTable;

  protected TeamController(
    Key secretKey, UserDatabaseTable userDatabaseTable,
    TeamDatabaseTable teamDatabaseTable, GroupDatabaseTable groupDatabaseTable
  ) {
    super(secretKey, userDatabaseTable);
    this.teamDatabaseTable = teamDatabaseTable;
    this.groupDatabaseTable = groupDatabaseTable;
  }

  protected boolean isAuthenticatedTeamMember(HttpServletRequest request) {
    return isValidApiKey(findApiKey(request));
  }

  protected boolean isValidApiKey(String apiKey) {
    try {
      var payload = Jwts.parser()
        .setSigningKey(secretKey())
        .build()
        .parseClaimsJws(apiKey).getPayload();
      return payload.containsKey("team");
    } catch (Exception exception) {
      return false;
    }
  }

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
