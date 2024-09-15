package com.dulno.team.distribution;


import com.dulno.core.configuration.Configuration;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.json.JSONObject;

@Getter
@Accessors(fluent = true)
public final class TeamDistributionConfiguration extends Configuration {
  private static final String CONFIGURATION_PATH = "/configurations/distribution/distribution.json";

  public static TeamDistributionConfiguration createAndLoad() throws Exception {
    var configuration = new TeamDistributionConfiguration(CONFIGURATION_PATH);
    configuration.load();
    return configuration;
  }

  private String distributionKey;

  private TeamDistributionConfiguration(String path) {
    super(path);
  }

  @Override
  protected void deserialize(JSONObject json) {
    distributionKey = json.getString("distributionKey");
  }
}
