package com.dulno.team.hetzner;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.json.JSONObject;

import java.util.Map;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class HetznerServer {
  public static HetznerServer of(JSONObject json) {
    var publicNetwork = json.getJSONObject("public_net");
    var serverType = json.getJSONObject("server_type");
    var location = json.getJSONObject("datacenter").getJSONObject("location");
    return create(json.getInt("id"), json.getString("name"),
      publicNetwork.getJSONObject("ipv4").getString("ip"),
      publicNetwork.getJSONObject("ipv6").getString("ip"),
      serverType.getString("description"), location.getString("city"),
      location.getString("country"), json.getString("status"),
      serverType.getString("architecture"), serverType.getInt("cores"),
      serverType.getString("cpu_type"), serverType.getInt("memory"),
      serverType.getInt("disk"));
  }

  private final int id;
  private final String name;
  private final String ipv4;
  private final String ipv6;
  private final String type;
  private final String locationCity;
  private final String locationCountry;
  private final String status;
  private final String architecture;
  private final int cores;
  private final String cpuType;
  private final int memory;
  private final int disk;

  public Map<String, Object> assemblyInformation() {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", id);
    information.put("name", name);
    information.put("ipv4", ipv4);
    information.put("ipv6", ipv6);
    information.put("type", type);
    information.put("locationCity", locationCity);
    information.put("locationCountry", locationCountry);
    information.put("status", status);
    information.put("architecture", architecture);
    information.put("cores", cores);
    information.put("cpuType", cpuType);
    information.put("memory", memory);
    information.put("disk", disk);
    return information;
  }
}
