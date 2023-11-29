/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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
import io.mapsmessaging.rest.auth.AuthenticationFilter;
import io.mapsmessaging.rest.translation.DebugMapper;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import jakarta.servlet.Servlet;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.ServerConfiguration;
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
import java.io.IOException;
import java.net.URI;

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
    if(serviceInfos != null) {
      for (ServiceInfo serviceInfo : serviceInfos) {
        httpServer.shutdown();
        MessageDaemon.getInstance().getDiscoveryManager().deregister(serviceInfo);
      }
    }
  }

  private void loadStatic(){
    if(map.containsKey("static")){
      Object obj = map.get("static");
      if(obj instanceof ConfigurationProperties){
        ConfigurationProperties staticConfig = (ConfigurationProperties) obj;
        if(staticConfig.getBooleanProperty("enabled", false)){
          String path = staticConfig.getProperty("path", "./html");
          StaticHttpHandler staticHttpHandler = new StaticHttpHandler(path);
          httpServer.getServerConfiguration().addHttpHandler(staticHttpHandler, "/");
        }
      }
    }
  }

  public void startServer() {
    try {
      final ResourceConfig config = new ResourceConfig();
      config.packages(true,
          "io.swagger.v3.jaxrs2.integration.resources",
          "io.swagger.v3.jaxrs2.integration.resources.AcceptHeaderOpenApiResource",
          "io.mapsmessaging.rest.api.impl",
          "io.mapsmessaging.rest.api",
          "io.mapsmessaging.rest.translation"
      );
      if(map.getBooleanProperty("enableSwagger", false)) {
      }
      boolean enableAuth = map.getBooleanProperty("enableAuthentication", false);
      if (enableAuth && AuthManager.getInstance().isAuthenticationEnabled()) {
        config.register(new AuthenticationFilter());
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
      loadStatic();
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
        path = String.format("/%s", (UriComponent.decodePath(uri.getPath(), true).get(1)).toString());
        WebappContext context = new WebappContext("GrizzlyContext", path);
        ServletRegistration registration = context.addServlet(servlet.getClass().getName(), servlet);

        registration.addMapping("/*");
        HttpServer server;
        if(sslConfig != null) {
          SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(sslConfig, false, false, false);
          server = GrizzlyHttpServerFactory.createHttpServer(uri,  ((GrizzlyHttpContainer)null), true,sslEngineConfigurator, false);
        }
        else{
          server = GrizzlyHttpServerFactory.createHttpServer(uri);
        }
        context.deploy(server);
        return server;
      }
    }
  }
}
