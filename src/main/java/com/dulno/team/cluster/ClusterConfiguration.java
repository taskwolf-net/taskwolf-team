package com.dulno.team.cluster;

import com.dulno.core.configuration.Configuration;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.json.JSONObject;

@Getter
@Accessors(fluent = true)
public final class ClusterConfiguration extends Configuration {
  private static final String CONFIGURATION_PATH = "/configurations/cluster/cluster.json";

  public static ClusterConfiguration createAndLoad() throws Exception {
    var configuration = new ClusterConfiguration(CONFIGURATION_PATH);
    configuration.load();
    return configuration;
  }

  private String namespace;
  private String databaseController;
  private String databasePath;
  private String databaseContainer;
  private String backupEncryptionKeyPath;
  private String backupEncryptionKeyName;
  private String backupDecryptionKeyPath;
  private String backupDecryptionKeyName;

  private ClusterConfiguration(String path) {
    super(path);
  }

  @Override
  protected void deserialize(JSONObject json) {
    namespace = json.getString("namespace");
    databaseController = json.getString("databaseController");
    databasePath = json.getString("databasePath");
    databaseContainer = json.getString("databaseContainer");
    backupEncryptionKeyPath = json.getString("backupEncryptionKeyPath");
    backupEncryptionKeyName = json.getString("backupEncryptionKeyName");
    backupDecryptionKeyPath = json.getString("backupDecryptionKeyPath");
    backupDecryptionKeyName = json.getString("backupDecryptionKeyName");
  }
}