package io.mapsmessaging.rest;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.rest.translation.DebugMapper;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.io.IOException;
import java.net.URI;
import javax.jmdns.ServiceInfo;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.servlet.GrizzlyWebContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

public class RestApiServerManager implements Agent {

  private final ConfigurationProperties map;

  private boolean isSecure;
  private ServiceInfo[] serviceInfos;
  private HttpServer httpServer;

  public RestApiServerManager() {
    map = ConfigurationManager.getInstance().getProperties("RestApi");
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
      setupSSL();
      startServer();
      serviceInfos = MessageDaemon.getInstance().getDiscoveryManager().register(this);
    }
  }


  public int getPort() {
    return map.getIntProperty("port", 4567);
  }

  public String getHost() {
    return map.getProperty("host", "0.0.0.0");
  }

  public boolean isSecure() {
    return isSecure;
  }

  private void setupSSL() {
    String keyStore = map.getProperty("ssl_keyStoreFile", null);
    String keyStorePass = map.getProperty("ssl_keyStorePassphrase", null);
    String trustStore = map.getProperty("ssl_trustStoreFile", null);
    String trustStorePass = map.getProperty("ssl_trustStorePassphrase", null);
    if (keyStore != null &&
        keyStorePass != null &&
        trustStore != null &&
        trustStorePass != null
    ) {
      isSecure = true;
    }
  }

  public void stop() {
    for (ServiceInfo serviceInfo : serviceInfos) {
      httpServer.shutdown();
      MessageDaemon.getInstance().getDiscoveryManager().deregister(serviceInfo);
    }
  }


  public void startServer() {
    try {

      final ResourceConfig config = new ResourceConfig();
      config.packages(true, "io.mapsmessaging.rest.api.impl", "io.mapsmessaging.rest.api", "io.mapsmessaging.rest.translation");
      config.register(io.swagger.jaxrs.listing.ApiListingResource.class);
      config.register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
      config.register(DebugMapper.class);
      ServletContainer sc = new ServletContainer(config);

      String baseUri = "http://" + getHost() + ":" + getPort() + "/";
      httpServer = GrizzlyWebContainerFactory.create(URI.create(baseUri), sc, null, null);
      httpServer.start();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
