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
import org.apache.commons.compress.utils.Lists;
import org.json.JSONArray;
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
      .thenApply(response -> assemblyClusterInformation(
        new JSONObject(response.body()).getJSONArray("servers")));
  }

  private Map<String, Object> assemblyClusterInformation(JSONArray servers) {
    var information = Lists.<Map<String, Object>>newArrayList();
    for (var i = 0; i < servers.length(); i++) {
      var entry = servers.getJSONObject(i);
      information.add(HetznerServer.of(entry).assemblyInformation());
    }
    return Map.of("servers", information);
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
    HetznerServer server, Map<String, Object> cpuMetrics,
    Map<String, Object> diskMetrics, Map<String, Object> networkMetrics
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.putAll(server.assemblyInformation());
    information.putAll(cpuMetrics);
    information.putAll(diskMetrics);
    information.putAll(networkMetrics);
    return information;
  }

  private CompletableFuture<HetznerServer> fetchServer(int serverId) {
    return hetznerRequestFactory.create().send("/servers/" + serverId, "GET")
      .thenApply(response -> HetznerServer.of(new JSONObject(response.body())
        .getJSONObject("server")));
  }

  private CompletableFuture<Map<String, Object>> fetchServerMetrics(
    int serverId, String type
  ) {
    var format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    format.setTimeZone(TimeZone.getTimeZone("UTC"));
    var time = format.format(new Date());
    return hetznerRequestFactory.create().send("/servers/" + serverId +
        "/metrics?type=" + type + "&start=" + time + "&end=" + time, "GET")
      .thenApply(response ->selectServerMetrics(type, new JSONObject(response.body())
        .getJSONObject("metrics").getJSONObject("time_series")));
  }

  private Map<String, Object> selectServerMetrics(String type, JSONObject data) {
    var information = Maps.<String, Object>newHashMap();
    if (type.equalsIgnoreCase("cpu")) {
      information.put("cpu", readServerMetric(data, "cpu"));
    } else if (type.equalsIgnoreCase("disk")) {
      information.put("diskReadIops", readServerMetric(data, "disk.0.iops.read"));
      information.put("diskWriteIops", readServerMetric(data, "disk.0.iops.write"));
      information.put("diskReadBandwidth",
        readServerMetric(data, "disk.0.bandwidth.read"));
      information.put("diskWriteBandwidth",
        readServerMetric(data, "disk.0.bandwidth.write"));
    } else if (type.equalsIgnoreCase("network")) {
      information.put("networkInPps", readServerMetric(data, "network.0.pps.in"));
      information.put("networkOutPps", readServerMetric(data, "network.0.pps.out"));
      information.put("networkInBandwidth",
        readServerMetric(data, "network.0.bandwidth.in"));
      information.put("networkOutBandwidth",
        readServerMetric(data, "network.0.bandwidth.out"));
    }
    return information;
  }

  private double readServerMetric(JSONObject data, String metric) {
    return data.getJSONObject(metric).getJSONArray("values").getJSONArray(0)
      .getDouble(1);
  }
}
