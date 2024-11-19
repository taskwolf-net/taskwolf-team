package com.dulno.team.access.service;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Key;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class ServiceController extends TeamController {
  private final HttpClient httpClient = HttpClient.newHttpClient();

  private ServiceController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
  }

  @RequestMapping(path = "/service/available/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> isServiceAvailable(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var requestBuilder = HttpRequest.newBuilder()
      .uri(URI.create(body.getString("url")))
      .GET();
    var sendTime = System.currentTimeMillis();
    var futureResponse = httpClient.sendAsync(requestBuilder.build(),
      HttpResponse.BodyHandlers.ofString());
    return futureResponse.thenApply(result -> Map.of("available",
      result.statusCode() >= 200 && result.statusCode() < 400, "delay",
      System.currentTimeMillis() - sendTime));
  }
}
