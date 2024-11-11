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

import static io.mapsmessaging.logging.ServerLogMessages.REST_API_FAILURE;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.config.RestApiManagerConfig;
import io.mapsmessaging.config.network.impl.TlsConfig;
import io.mapsmessaging.config.rest.StaticConfig;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.rest.auth.AuthenticationContext;
import io.mapsmessaging.rest.auth.AuthenticationFilter;
import io.mapsmessaging.rest.auth.RestAccessControl;
import io.mapsmessaging.rest.translation.DebugMapper;
import io.mapsmessaging.rest.translation.GsonMessageBodyReader;
import io.mapsmessaging.rest.translation.GsonMessageBodyWriter;
import io.mapsmessaging.utilities.Agent;
import jakarta.servlet.Servlet;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.jmdns.ServiceInfo;
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

public class RestApiServerManager implements Agent {

  private RestApiManagerConfig config;
  private final Logger logger = LoggerFactory.getLogger(RestApiServerManager.class);

  private boolean isSecure;
  private ServiceInfo[] serviceInfos;
  private HttpServer httpServer;

  public RestApiServerManager() {
    config = RestApiManagerConfig.getInstance();
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
    if (config.isEnabled()) {
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
    return config.getPort();
  }

  public String getHost() {
    return config.getHostnames();
  }

  public boolean isSecure() {
    return isSecure;
  }

  private SSLContextConfigurator setupSSL() {
    TlsConfig sslConfig = config.getTlsConfig();
    if (sslConfig != null) {
      isSecure = true;
      SSLContextConfigurator sslCon = new SSLContextConfigurator();
      sslCon.setKeyStoreFile(sslConfig.getSslConfig().getKeyStore().getPath());
      sslCon.setKeyStorePass(sslConfig.getSslConfig().getKeyStore().getPassphrase());
      sslCon.setTrustStoreFile(sslConfig.getSslConfig().getTrustStore().getPath());
      sslCon.setTrustStorePass(sslConfig.getSslConfig().getTrustStore().getPassphrase());
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

  public void startServer() {
    List<String> endpoints = new ArrayList<>();
    endpoints.add("io.mapsmessaging.rest.api.impl");
    endpoints.add("io.mapsmessaging.rest.api.impl.messaging");
    endpoints.add("io.mapsmessaging.rest.translation");

    if (config.isEnableSwagger()) {
      endpoints.add("io.swagger.v3.jaxrs2.integration.resources");
      endpoints.add("io.swagger.v3.jaxrs2.integration.resources.AcceptHeaderOpenApiResource");
    }
    if (config.isEnableUserManagement()) {
      endpoints.add("io.mapsmessaging.rest.api.impl.auth");
    }
    if (config.isEnableDestinationManagement()) {
      endpoints.add("io.mapsmessaging.rest.api.impl.destination");
    }
    if (config.isEnableInterfaceManagement()) {
      endpoints.add("io.mapsmessaging.rest.api.impl.interfaces");
      endpoints.add("io.mapsmessaging.rest.api.impl.integration");
      endpoints.add("io.mapsmessaging.rest.api.impl.connections");
      endpoints.add("io.mapsmessaging.rest.api.impl.lora");
      endpoints.add("io.mapsmessaging.rest.api.impl.discovery");
    }
    if (config.isEnableSchemaManagement()) {
      endpoints.add("io.mapsmessaging.rest.api.impl.schema");
    }
    endpoints.add("io.mapsmessaging.rest.api.impl.server");
    try {
      final ResourceConfig resourceConfig = new ResourceConfig();
      resourceConfig.packages(false, endpoints.toArray(new String[0]));
      // Register Gson providers
      resourceConfig.register(GsonMessageBodyReader.class);
      resourceConfig.register(GsonMessageBodyWriter.class);

      if (config.isEnableAuthentication() && AuthManager.getInstance().isAuthenticationEnabled()) {
        resourceConfig.register(new AuthenticationFilter());
        AuthenticationContext.getInstance().setAccessControl(new RestAccessControl());
      }
      resourceConfig.register(DebugMapper.class );
      resourceConfig.register(LoggingFilter.class);
      ServletContainer sc = new ServletContainer(resourceConfig);
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
    if (config.getStaticConfig() != null){
      StaticConfig staticConfig = config.getStaticConfig();
      if(staticConfig.isEnabled()){
        String path = staticConfig.getDirectory();
        if (!path.endsWith(File.separator)) {
          path = path + File.separator;
        }
        mapDirectory(path, server );
      }
    }
  }


  private void mapDirectory(String path, HttpServer server) {
    File files = new File(path);
    if(files.exists() && files.isDirectory()) {
      File[] fileList = files.listFiles();
      if (fileList != null) {
        for (File file : fileList) {
          if (file.isDirectory()) {
            String location = path+file.getName();
            String uriMapping = "/"+file.getName()+"/";
            StaticHttpHandler staticHttpHandler = new StaticHttpHandler(location+File.separator);
            staticHttpHandler.setFileCacheEnabled(true);
            server.getServerConfiguration().addHttpHandler(staticHttpHandler, uriMapping+"*" );
          }
        }
      }
    }
  }

}
