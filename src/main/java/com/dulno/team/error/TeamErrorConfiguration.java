package com.dulno.team.error;

import com.dulno.core.configuration.Configuration;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.json.JSONObject;

@Getter
@Accessors(fluent = true)
public final class TeamErrorConfiguration extends Configuration {
  private static final String CONFIGURATION_PATH = "/configurations/team/team-error.json";

  public static TeamErrorConfiguration createAndLoad() throws Exception {
    var configuration = new TeamErrorConfiguration(CONFIGURATION_PATH);
    configuration.load();
    return configuration;
  }

  private String reportKey;

  private TeamErrorConfiguration(String path) {
    super(path);
  }

  @Override
  protected void deserialize(JSONObject json) {
    reportKey = json.getString("reportKey");
  }
}