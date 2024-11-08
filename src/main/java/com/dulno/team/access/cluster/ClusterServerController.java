package com.dulno.team.access.cluster;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.hetzner.HetznerRequestFactory;
import com.dulno.team.hetzner.HetznerServer;
import com.dulno.team.member.TeamDatabaseTable;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;

@RestController
public final class ClusterServerController extends TeamController {
  private final HetznerRequestFactory hetznerRequestFactory;

  private ClusterServerController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    HetznerRequestFactory hetznerRequestFactory
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.hetznerRequestFactory = hetznerRequestFactory;
  }

  @RequestMapping(path = "/cluster/servers/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findClusterServers(
    HttpServletRequest request
  ) {
    return memberHasPermission(findMemberId(request), "cluster.servers")
      .thenCompose(this::findClusterServers);
  }

  private CompletableFuture<Map<String, Object>> findClusterServers(
    boolean hasPermission
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return hetznerRequestFactory.create().send("/servers", "GET")
      .thenApply(response -> new JSONObject(response.body()).getJSONArray("servers"))
      .thenApply(array -> array.toList().stream().map(entry -> (JSONObject) entry)
        .map(HetznerServer::of).map(HetznerServer::assemblyInformation).toList())
      .thenApply(information -> Map.of("servers", information));
  }

  @RequestMapping(path = "/cluster/server/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> findClusterServer(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    return memberHasPermission(findMemberId(request), "cluster.server")
      .thenCompose(hasPermission -> findClusterServer(body.getInt("serverId"),
        hasPermission));
  }

  private CompletableFuture<Map<String, Object>> findClusterServer(
    int serverId, boolean hasPermission
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return fetchServer(serverId)
      .thenCompose(server -> fetchServerMetrics(serverId, "cpu")
        .thenCompose(cpuMetrics -> fetchServerMetrics(serverId, "disk")
          .thenCompose(diskMetrics -> fetchServerMetrics(serverId, "network")
            .thenApply(networkMetrics -> assemblyClusterServerInformation(server,
              cpuMetrics, diskMetrics, networkMetrics)))));
  }

  private Map<String, Object> assemblyClusterServerInformation(
    HetznerServer server, double cpuMetrics, double diskMetrics,
    double networkMetrics
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.putAll(server.assemblyInformation());
    information.put("cpu", cpuMetrics);
    information.put("disk", diskMetrics);
    information.put("network", networkMetrics);
    return information;
  }

  private CompletableFuture<HetznerServer> fetchServer(int serverId) {
    return hetznerRequestFactory.create().send("/servers/" + serverId, "GET")
      .thenApply(response -> HetznerServer.of(new JSONObject(response.body())
        .getJSONObject("server")));
  }

  private CompletableFuture<Double> fetchServerMetrics(int serverId, String type) {
    var format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    format.setTimeZone(TimeZone.getTimeZone("UTC"));
    var time = format.format(new Date());
    return hetznerRequestFactory.create().send("/servers/" + serverId +
        "/metrics?type=" + type + "&start=" + time + "&end=" + time, "GET")
      .thenApply(response -> new JSONObject(response.body()).getJSONObject("metrics")
        .getJSONObject("time_series").getJSONObject("name_of_timeseries")
        .getJSONArray("values").getJSONArray(0).getDouble(0));
  }
}
