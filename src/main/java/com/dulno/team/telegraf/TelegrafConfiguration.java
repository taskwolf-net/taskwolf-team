package com.dulno.team.telegraf;

import com.dulno.core.configuration.Configuration;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.json.JSONObject;

@Getter
@Accessors(fluent = true)
public final class TelegrafConfiguration extends Configuration {
  private static final String CONFIGURATION_PATH = "/configurations/telegraf/telegraf.json";

  public static TelegrafConfiguration createAndLoad() throws Exception {
    var configuration = new TelegrafConfiguration(CONFIGURATION_PATH);
    configuration.load();
    return configuration;
  }

  private String kubernetesNamespace;
  private String telegrafCluster;
  private int telegrafPort;

  private TelegrafConfiguration(String path) {
    super(path);
  }

  @Override
  protected void deserialize(JSONObject json) {
    kubernetesNamespace = json.getString("kubernetesNamespace");
    telegrafCluster = json.getString("telegrafCluster");
    telegrafPort = json.getInt("telegrafPort");
  }
}