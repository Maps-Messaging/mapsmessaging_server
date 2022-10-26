package io.mapsmessaging.rest;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.rest.translation.DebugMapper;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.io.IOException;
import java.net.URI;
import javax.jmdns.ServiceInfo;
import javax.servlet.Servlet;
import javax.ws.rs.core.PathSegment;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.uri.UriComponent;

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

  private SSLContextConfigurator setupSSL() {
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
      SSLContextConfigurator sslCon = new SSLContextConfigurator();
      sslCon.setKeyStoreFile(keyStore);
      sslCon.setKeyStorePass(keyStorePass);
      sslCon.setTrustStoreFile(trustStore);
      sslCon.setTrustStorePass(trustStorePass);
      return sslCon;
    }
    return null;
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
      if(map.getBooleanProperty("enableSwagger", false)) {
        config.register(io.swagger.jaxrs.listing.ApiListingResource.class);
        config.register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
      }
      config.register(DebugMapper.class);
      ServletContainer sc = new ServletContainer(config);

      SSLContextConfigurator sslConfig = setupSSL();
      String protocol = "http";
      if(isSecure){
        protocol = "https";
      }
      String baseUri = protocol+"://" + getHost() + ":" + getPort() + "/";

      httpServer = startHttpService(URI.create(baseUri), sc, sslConfig);
      httpServer.start();
      if(map.getBooleanProperty("enableSwaggerUI", false) && map.getBooleanProperty("enableSwagger", false)) {
        ServerConfiguration cfg = httpServer.getServerConfiguration();
        ClassLoader loader = RestApiServerManager.class.getClassLoader();
        CLStaticHttpHandler docsHandler = new CLStaticHttpHandler(loader, "swagger-ui/");
        docsHandler.setFileCacheEnabled(false);
        cfg.addHttpHandler(docsHandler, "/swagger-ui/");
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private HttpServer startHttpService(URI uri, Servlet servlet, SSLContextConfigurator sslConfig){
    if (uri == null) {
      throw new IllegalArgumentException("The URI must not be null");
    } else {
      String path = uri.getPath();
      if (path == null) {
        throw new IllegalArgumentException("The URI path, of the URI " + uri + ", must be non-null");
      } else if (path.isEmpty()) {
        throw new IllegalArgumentException("The URI path, of the URI " + uri + ", must be present");
      } else if (path.charAt(0) != '/') {
        throw new IllegalArgumentException("The URI path, of the URI " + uri + ". must start with a '/'");
      } else {
        path = String.format("/%s", ((PathSegment) UriComponent.decodePath(uri.getPath(), true).get(1)).toString());
        WebappContext context = new WebappContext("GrizzlyContext", path);
        ServletRegistration registration = context.addServlet(servlet.getClass().getName(), servlet);

        registration.addMapping(new String[]{"/*"});
        HttpServer server;
        if(sslConfig != null) {
          SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(sslConfig, false, false, false);
          server = GrizzlyHttpServerFactory.createHttpServer(uri,  (GrizzlyHttpContainer)((GrizzlyHttpContainer)null), true,sslEngineConfigurator, false);
        }
        else{
          server =GrizzlyHttpServerFactory.createHttpServer(uri);
        }
        context.deploy(server);
        return server;
      }
    }
  }
}
