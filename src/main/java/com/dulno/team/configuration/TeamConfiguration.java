package com.dulno.team.configuration;

import lombok.Getter;
import lombok.experimental.Accessors;
import com.dulno.core.configuration.Configuration;
import org.json.JSONObject;

@Getter
@Accessors(fluent = true)
public final class TeamConfiguration extends Configuration {
  private static final String CONFIGURATION_PATH = "/configurations/team/team.json";

  public static TeamConfiguration createAndLoad() throws Exception {
    var configuration = new TeamConfiguration(CONFIGURATION_PATH);
    configuration.load();
    return configuration;
  }

  private int restPort;
  private String secret;

  private TeamConfiguration(String path) {
    super(path);
  }

  @Override
  protected void deserialize(JSONObject json) {
    restPort = json.getInt("restPort");
    secret = json.getString("secret");
  }
}