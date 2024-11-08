package com.dulno.team.hetzner;

import com.dulno.core.configuration.Configuration;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.json.JSONObject;

@Getter
@Accessors(fluent = true)
public final class HetznerConfiguration extends Configuration {
  private static final String CONFIGURATION_PATH = "/configurations/hetzner/hetzner.json";

  public static HetznerConfiguration createAndLoad() throws Exception {
    var configuration = new HetznerConfiguration(CONFIGURATION_PATH);
    configuration.load();
    return configuration;
  }

  private String apiToken;

  private HetznerConfiguration(String path) {
    super(path);
  }

  @Override
  protected void deserialize(JSONObject json) {
    apiToken = json.getString("apiToken");
  }
}