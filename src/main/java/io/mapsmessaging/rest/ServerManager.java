package io.mapsmessaging.rest;

import static spark.Spark.ipAddress;
import static spark.Spark.port;

import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.util.ServiceLoader;

public class ServerManager {
  private final ServiceLoader<RestApi> restApis;

  public ServerManager(){
    restApis = ServiceLoader.load(RestApi.class);
  }

  public void initRestApi(ConfigurationProperties map){
    if(map.getBooleanProperty("enabled", false)) {
      port(map.getIntProperty("port", 4567));
      ipAddress(map.getProperty("host", "0.0.0.0"));
      for(RestApi restApi:restApis){
        restApi.initialise();
      }
    }
  }

}
