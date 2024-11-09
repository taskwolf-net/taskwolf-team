package com.dulno.team.prometheus;

import com.dulno.core.configuration.Configuration;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.json.JSONObject;

@Getter
@Accessors(fluent = true)
public final class PrometheusConfiguration extends Configuration {
  private static final String CONFIGURATION_PATH = "/configurations/prometheus/prometheus.json";

  public static PrometheusConfiguration createAndLoad() throws Exception {
    var configuration = new PrometheusConfiguration(CONFIGURATION_PATH);
    configuration.load();
    return configuration;
  }

  private String kubernetesNamespace;
  private int exporterPort;
  private String rootMountPath;

  private PrometheusConfiguration(String path) {
    super(path);
  }

  @Override
  protected void deserialize(JSONObject json) {
    kubernetesNamespace = json.getString("kubernetesNamespace");
    exporterPort = json.getInt("exporterPort");
    rootMountPath = json.getString("rootMountPath");
  }
}