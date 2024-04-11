/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.rest;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.rest.auth.AuthenticationFilter;
import io.mapsmessaging.rest.translation.DebugMapper;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import jakarta.servlet.Servlet;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.uri.UriComponent;

import javax.jmdns.ServiceInfo;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static io.mapsmessaging.logging.ServerLogMessages.REST_API_FAILURE;

public class RestApiServerManager implements Agent {

  private final ConfigurationProperties map;
  private final Logger logger = LoggerFactory.getLogger(RestApiServerManager.class);

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
      Thread t= new Thread(() -> {
        setupSSL();
        startServer();
        serviceInfos = MessageDaemon.getInstance().getDiscoveryManager().register(this);
      });
      t.setDaemon(true);
      t.start();
    }
  }


  public int getPort() {
    return map.getIntProperty("port", 4567);
  }

  public String getHost() {
    return map.getProperty("hostnames", "0.0.0.0");
  }

  public boolean isSecure() {
    return isSecure;
  }

  private SSLContextConfigurator setupSSL() {
    ConfigurationProperties securityProps = (ConfigurationProperties) map.get("security");
    if (securityProps != null && securityProps.containsKey("tls")) {
      ConfigurationProperties tls = (ConfigurationProperties) securityProps.get("tls");
      ConfigurationProperties keyStoreProps = (ConfigurationProperties) tls.get("keyStore");
      ConfigurationProperties trustStoreProps = (ConfigurationProperties) tls.get("trustStore");

      String keyStore = keyStoreProps.getProperty("file", null);
      String keyStorePass = keyStoreProps.getProperty("passphrase", null);

      String trustStore = trustStoreProps.getProperty("file", null);
      String trustStorePass = trustStoreProps.getProperty("passphrase", null);

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
    }
    return null;
  }

  public void stop() {
    if(serviceInfos != null) {
      for (ServiceInfo serviceInfo : serviceInfos) {
        httpServer.shutdown();
        MessageDaemon.getInstance().getDiscoveryManager().deregister(serviceInfo);
      }
    }
  }

  public void startServer() {
    boolean enableSwagger = map.getBooleanProperty("enableSwagger", true);
    boolean enableUserManagement = map.getBooleanProperty("enableUserManagement", true);
    boolean enableSchemaManagement = map.getBooleanProperty("enableSchemaManagement", true);
    boolean enableInterfaceManagement = map.getBooleanProperty("enableInterfaceManagement", true);
    boolean enableDestinationManagement = map.getBooleanProperty("enableDestinationManagement", true);
    List<String> endpoints = new ArrayList<>();
    endpoints.add("io.mapsmessaging.rest.api.impl");
    endpoints.add("io.mapsmessaging.rest.api.impl.messaging");
    endpoints.add("io.mapsmessaging.rest.translation");

    if (enableSwagger) {
      endpoints.add("io.swagger.v3.jaxrs2.integration.resources");
      endpoints.add("io.swagger.v3.jaxrs2.integration.resources.AcceptHeaderOpenApiResource");
    }
    if (enableUserManagement) {
      endpoints.add("io.mapsmessaging.rest.api.impl.auth");
    }
    if (enableDestinationManagement) {
      endpoints.add("io.mapsmessaging.rest.api.impl.destination");
    }
    if (enableInterfaceManagement) {
      endpoints.add("io.mapsmessaging.rest.api.impl.interfaces");
    }
    if (enableSchemaManagement) {
      endpoints.add("io.mapsmessaging.rest.api.impl.schema");
    }

    try {
      final ResourceConfig config = new ResourceConfig();
      config.packages(false, endpoints.toArray(new String[0]));
      boolean enableAuth = map.getBooleanProperty("enableAuthentication", false);
      if (enableAuth && AuthManager.getInstance().isAuthenticationEnabled()) {
        config.register(new AuthenticationFilter());
      }
      config.register(DebugMapper.class );
      config.register(LoggingFilter.class);
      ServletContainer sc = new ServletContainer(config);
      SSLContextConfigurator sslConfig = setupSSL();
      String protocol = "http";
      if(isSecure){
        protocol = "https";
      }
      String baseUri = protocol+"://" + getHost() + ":" + getPort() + "/";

      httpServer = startHttpService(URI.create(baseUri), sc, sslConfig);

      httpServer.start();
    } catch (IOException e) {
      logger.log(REST_API_FAILURE, e);
    }
  }

  public HttpServer startHttpService(URI uri, Servlet servlet, SSLContextConfigurator sslConfig) {
    String path = uri.getPath();
    if (path == null || path.isEmpty() || path.charAt(0) != '/') {
      throw new IllegalArgumentException("The URI path, of the URI " + uri + ", must be non-null, present and start with a '/'");
    }

    path = String.format("/%s", (UriComponent.decodePath(uri.getPath(), true).get(1)).toString());
    WebappContext context = new WebappContext("GrizzlyContext", path);
    ServletRegistration registration = context.addServlet(servlet.getClass().getName(), servlet);

    registration.addMapping("/*");
    HttpServer server;
    if (sslConfig != null) {
      SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(sslConfig, false, false, false);
      server = GrizzlyHttpServerFactory.createHttpServer(uri, ((GrizzlyHttpContainer) null), true, sslEngineConfigurator, false);
    } else {
      server = GrizzlyHttpServerFactory.createHttpServer(uri);
    }
    context.deploy(server);
    loadStatic(server);
    return server;
  }

  private void loadStatic(HttpServer server){
    if(map.containsKey("static")){
      Object obj = map.get("static");
      if(obj instanceof ConfigurationProperties){
        ConfigurationProperties staticConfig = (ConfigurationProperties) obj;
        if(staticConfig.getBooleanProperty("enabled", false)){
          String path = staticConfig.getProperty("directory", "./www");
          if (!path.endsWith(File.separator)) {
            path = path + File.separator;
          }
          StaticHttpHandler staticHttpHandler = new StaticHttpHandler(path);
          staticHttpHandler.setFileCacheEnabled(false);
          server.getServerConfiguration().addHttpHandler(staticHttpHandler, "/admin/*");

          if (map.getBooleanProperty("enableSwaggerUI", false) && map.getBooleanProperty("enableSwagger", false)) {
            StaticHttpHandler swaggerHttpHandler = new StaticHttpHandler(path + "swagger-ui");
            swaggerHttpHandler.setFileCacheEnabled(true);
            server.getServerConfiguration().addHttpHandler(swaggerHttpHandler, "/swagger-ui/*");
          }
        }
      }
    }
  }

}
