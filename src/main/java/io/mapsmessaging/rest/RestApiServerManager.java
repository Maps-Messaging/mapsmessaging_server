package io.mapsmessaging.rest;

import static spark.Spark.ipAddress;
import static spark.Spark.port;
import static spark.Spark.secure;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.util.ServiceLoader;
import javax.jmdns.ServiceInfo;

public class RestApiServerManager implements Agent {

  private final ServiceLoader<RestApi> restApis;
  private final ConfigurationProperties map;

  private boolean isSecure;
  private ServiceInfo[] serviceInfos;

  public RestApiServerManager() {
    map = ConfigurationManager.getInstance().getProperties("RestApi");
    restApis = ServiceLoader.load(RestApi.class);
    isSecure = false;
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
      port( map.getIntProperty("port", 4567));
      ipAddress( map.getProperty("host", "0.0.0.0"));
      setupSSL();
      for (RestApi restApi : restApis) {
        restApi.initialise();
      }
      serviceInfos = MessageDaemon.getInstance().getDiscoveryManager().register(this);
    }
  }

  public int getPort(){
    return map.getIntProperty("port", 4567);
  }

  public String getHost(){
    return map.getProperty("host", "0.0.0.0");
  }

  public boolean isSecure(){
    return isSecure;
  }

  private void setupSSL(){
    String keyStore = map.getProperty("ssl_keyStoreFile", null);
    String keyStorePass = map.getProperty("ssl_keyStorePassphrase", null);
    String trustStore = map.getProperty("ssl_trustStoreFile", null);
    String trustStorePass = map.getProperty("ssl_trustStorePassphrase", null);
    if(keyStore != null &&
        keyStorePass != null &&
        trustStore != null &&
        trustStorePass != null
    ){
      secure(keyStore, keyStorePass, trustStore, trustStorePass);
      isSecure = true;
    }
  }

  public void stop() {
    for(ServiceInfo serviceInfo: serviceInfos) {
      MessageDaemon.getInstance().getDiscoveryManager().deregister(serviceInfo);
    }
  }

}
