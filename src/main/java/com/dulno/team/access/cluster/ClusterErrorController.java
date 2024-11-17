package com.dulno.team.access.cluster;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.error.DulnoError;
import com.dulno.core.error.DulnoErrorState;
import com.dulno.core.error.ErrorDatabaseTable;
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
public final class ClusterErrorController extends TeamController {
  private final ErrorDatabaseTable errorDatabaseTable;

  private ClusterErrorController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, ErrorDatabaseTable errorDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.errorDatabaseTable = errorDatabaseTable;
  }

  @RequestMapping(path = "/cluster/errors/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findClusterErrors(
    HttpServletRequest request
  ) {
    return memberHasPermission(findMemberId(request), "cluster.errors")
      .thenCompose(this::findClusterErrors);
  }

  private CompletableFuture<Map<String, Object>> findClusterErrors(
    boolean hasPermission
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return errorDatabaseTable.findErrorsByState(DulnoErrorState.UNSOLVED)
      .thenApply(errors -> Map.of("errors",
        errors.stream().map(this::assemblyErrorInformation).toList()));
  }

  private Map<String, Object> assemblyErrorInformation(DulnoError error) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", error.id());
    information.put("title", error.title());
    information.put("trace", error.trace());
    information.put("origin", error.origin());
    information.put("pod", error.pod());
    information.put("podControllerName", error.podControllerName());
    information.put("podControllerType", error.podControllerType());
    information.put("node", error.node());
    information.put("time", error.time());
    information.put("state", error.state());
    return information;
  }

  @RequestMapping(path = "/cluster/error/fixed/", method = RequestMethod.POST)
  public void fixedClusterError(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var errorId = body.getUUID("error");
    memberHasPermission(findMemberId(request), "cluster.error.fixed")
      .thenAccept(hasPermission -> errorDatabaseTable.errorExists(errorId)
        .thenAccept(exists -> fixedClusterError(errorId, hasPermission, exists)));
  }

  private void fixedClusterError(
    UUID errorId, boolean hasPermission, boolean errorExists
  ) {
    if (!hasPermission || !errorExists) {
      return;
    }
    errorDatabaseTable.findError(errorId).thenAccept(error ->
      errorDatabaseTable.updateErrorState(error, DulnoErrorState.FIXED));
  }

  @RequestMapping(path = "/cluster/errors/fixed/all/", method = RequestMethod.GET)
  public void fixedAllClusterErrors(HttpServletRequest request) {
    memberHasPermission(findMemberId(request), "cluster.errors.fixed.all")
      .thenAccept(this::fixedAllClusterErrors);
  }

  private void fixedAllClusterErrors(boolean hasPermission) {
    if (!hasPermission) {
      return;
    }
    errorDatabaseTable.findErrorsByState(DulnoErrorState.UNSOLVED)
      .thenAccept(errors -> errors.forEach(error ->
        errorDatabaseTable.updateErrorState(error, DulnoErrorState.FIXED)));
  }
}
