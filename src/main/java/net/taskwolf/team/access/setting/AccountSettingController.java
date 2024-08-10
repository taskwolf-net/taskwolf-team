package net.taskwolf.team.access.setting;

import com.google.common.hash.Hashing;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import net.taskwolf.team.member.TeamMember;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class AccountSettingController extends TeamController {
  private AccountSettingController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
  }

  @RequestMapping(path = "/settings/account/password/change/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> changePassword(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    findMember(request).thenAccept(member -> futureResponse.complete(
      changePassword(member, body.getString("currentPassword"),
        body.getString("newPassword"))));
    return futureResponse;
  }

  private Map<String, Object> changePassword(
    TeamMember member, String currentPassword, String newPassword
  ) {
    if (!member.passwordHash().equals(hashPassword(currentPassword))) {
      return Map.of("success", false);
    }
    teamDatabaseTable().changeTeamMemberPassword(member.id(),
      hashPassword(newPassword));
    return Map.of("success", true);
  }

  private String hashPassword(String password) {
    return Hashing.sha256().hashString(password, StandardCharsets.UTF_8)
      .toString();
  }
}
