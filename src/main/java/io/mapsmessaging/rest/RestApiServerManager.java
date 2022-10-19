package io.mapsmessaging.rest;

import static spark.Spark.ipAddress;
import static spark.Spark.port;

import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.util.ServiceLoader;

public class RestApiServerManager implements Agent {

  private final ServiceLoader<RestApi> restApis;
  private final ConfigurationProperties map;

  public RestApiServerManager() {
    map = ConfigurationManager.getInstance().getProperties("RestApi");
    restApis = ServiceLoader.load(RestApi.class);
  }

  @Override
  public String getName() {
    return "RestApi Manager";
  }

  @Override
  public String getDescription() {
    return "RestAPI management";
  }

  public void start() {
    if (map.getBooleanProperty("enabled", false)) {
      port(map.getIntProperty("port", 4567));
      ipAddress(map.getProperty("host", "0.0.0.0"));
      for (RestApi restApi : restApis) {
        restApi.initialise();
      }
    }
  }

  public void stop() {
  }

}
