package net.taskwolf.team.access.setting;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class LanguageSettingController extends TeamController {
  private LanguageSettingController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
  }

  @RequestMapping(path = "/settings/language/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> language(
    HttpServletRequest request
  ) {
    return findMember(request).thenApply(member ->
      Map.of("language", member.language()));
  }

  @RequestMapping(path = "/settings/language/change/", method = RequestMethod.POST)
  public void changeLanguage(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    teamDatabaseTable().changeTeamMemberLanguage(findMemberId(request),
      body.getString("language"));
  }
}
