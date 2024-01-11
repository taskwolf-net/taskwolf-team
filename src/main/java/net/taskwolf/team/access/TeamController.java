package net.taskwolf.team.access;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import net.taskwolf.core.access.TaskwolfRestController;
import net.taskwolf.core.user.UserDatabaseTable;

import java.security.Key;

public class TeamController extends TaskwolfRestController {
  protected TeamController(Key secretKey, UserDatabaseTable userDatabaseTable) {
    super(secretKey, userDatabaseTable);
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
}
