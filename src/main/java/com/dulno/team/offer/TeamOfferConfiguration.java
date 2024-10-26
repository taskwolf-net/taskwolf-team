package com.dulno.team.offer;

import com.dulno.core.configuration.Configuration;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.json.JSONObject;

@Getter
@Accessors(fluent = true)
public final class TeamOfferConfiguration extends Configuration {
  private static final String CONFIGURATION_PATH = "/configurations/team/team-offer.json";

  public static TeamOfferConfiguration createAndLoad() throws Exception {
    var configuration = new TeamOfferConfiguration(CONFIGURATION_PATH);
    configuration.load();
    return configuration;
  }

  private float workflowPrice;
  private float workflowOperationPrice;
  private float processPrice;
  private float databasePrice;
  private float databaseDataPrice;
  private float organizationMemberPrice;
  private float organizationTeamPrice;

  private TeamOfferConfiguration(String path) {
    super(path);
  }

  @Override
  protected void deserialize(JSONObject json) {
    workflowPrice = json.getFloat("workflowPrice");
    workflowOperationPrice = json.getFloat("workflowOperationPrice");
    processPrice = json.getFloat("processPrice");
    databasePrice = json.getFloat("databasePrice");
    databaseDataPrice = json.getFloat("databaseDataPrice");
    organizationMemberPrice = json.getFloat("organizationMemberPrice");
    organizationTeamPrice = json.getFloat("organizationTeamPrice");
  }
}