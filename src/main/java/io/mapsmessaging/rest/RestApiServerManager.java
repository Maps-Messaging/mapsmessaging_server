package io.mapsmessaging.rest;

import static spark.Spark.ipAddress;
import static spark.Spark.port;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.util.ServiceLoader;
import javax.jmdns.ServiceInfo;
import lombok.Getter;

public class RestApiServerManager implements Agent {

  private final ServiceLoader<RestApi> restApis;
  private final ConfigurationProperties map;

  @Getter
  private final int port;

  @Getter
  private final String host;

  private ServiceInfo[] serviceInfos;

  public RestApiServerManager() {
    map = ConfigurationManager.getInstance().getProperties("RestApi");
    restApis = ServiceLoader.load(RestApi.class);
    port = map.getIntProperty("port", 4567);
    host = map.getProperty("host", "0.0.0.0");
  }

  @Override
  public String getName() {
    return "Rest API Manager";
  }

  @Override
  public String getDescription() {
    return "Rest API management";
  }

  public void start() {
    if (map.getBooleanProperty("enabled", false)) {
      port(port);
      ipAddress(host);
      for (RestApi restApi : restApis) {
        restApi.initialise();
      }
      serviceInfos = MessageDaemon.getInstance().getDiscoveryManager().register(this);
    }
  }

  public void stop() {
    for(ServiceInfo serviceInfo: serviceInfos) {
      MessageDaemon.getInstance().getDiscoveryManager().deregister(serviceInfo);
    }
  }

}
