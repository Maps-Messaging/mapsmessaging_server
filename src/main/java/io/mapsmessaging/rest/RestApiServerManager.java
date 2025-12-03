/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.rest;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.config.RestApiManagerConfig;
import io.mapsmessaging.config.network.impl.TlsConfig;
import io.mapsmessaging.config.rest.StaticConfig;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.rest.api.Constants;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.api.impl.messaging.impl.RestMessageListener;
import io.mapsmessaging.rest.auth.*;
import io.mapsmessaging.rest.cache.impl.NoCache;
import io.mapsmessaging.rest.cache.impl.RoleBasedCache;
import io.mapsmessaging.rest.handler.CorsEnabledStaticHttpHandler;
import io.mapsmessaging.rest.handler.CorsFilter;
import io.mapsmessaging.rest.handler.SessionTracker;
import io.mapsmessaging.rest.translation.GsonMessageBodyReader;
import io.mapsmessaging.rest.translation.GsonMessageBodyWriter;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import jakarta.servlet.Servlet;
import lombok.Getter;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.uri.UriComponent;

import javax.jmdns.ServiceInfo;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static io.mapsmessaging.logging.ServerLogMessages.REST_API_FAILURE;

public class RestApiServerManager implements Agent {

  private final RestApiManagerConfig config;
  private final Logger logger = LoggerFactory.getLogger(RestApiServerManager.class);

  @Getter
  private String baseUri;

  @Getter
  private boolean isSecure;
  private ServiceInfo[] serviceInfos;
  private HttpServer httpServer;
  private ScheduledFuture cleanupSchedule;

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
        serviceInfos = MessageDaemon.getInstance().getSubSystemManager().getDiscoveryManager().register(this);
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

  private boolean isClientCertRequired() {
    TlsConfig sslConfig = config.getTlsConfig();
    if (sslConfig != null) {
      return sslConfig.getSslConfig().isClientCertificateRequired();
    }
    return false;
  }

  private boolean isClientCertWanted() {
    TlsConfig sslConfig = config.getTlsConfig();
    if (sslConfig != null) {
      return sslConfig.getSslConfig().isClientCertificateWanted();
    }
    return false;
  }

  public void stop() {
    if(cleanupSchedule != null) {
      cleanupSchedule.cancel(true);
    }
    if(serviceInfos != null) {
      for (ServiceInfo serviceInfo : serviceInfos) {
        httpServer.shutdown();
        MessageDaemon.getInstance().getSubSystemManager().getDiscoveryManager().deregister(serviceInfo);
      }
    }
  }

  public void startServer() {
    BaseRestApi.AUTH_ENABLED = config.isEnableAuthentication() && AuthManager.getInstance().isAuthorisationEnabled();

    RestMessageListener.setMaxSubscribedMessages(config.getMaxEventsPerDestination());
    int inactiveTimeout = config.getInactiveTimeout();
    if(inactiveTimeout < 10000) {
      inactiveTimeout = 180000;
    }
    BaseAuthenticationFilter.setMaxInactiveInterval(inactiveTimeout);
    if(config.isEnableCache()){
      long entryLifeTime = config.getCacheLifetime();
      long cleanupTime = config.getCacheCleanup();
      io.mapsmessaging.rest.api.Constants.setCentralCache( new RoleBasedCache<>(entryLifeTime, cleanupTime));
    }
    else{
      io.mapsmessaging.rest.api.Constants.setCentralCache(new NoCache<>());
    }
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
    endpoints.add("io.mapsmessaging.rest.api.impl.hardware");
    endpoints.add("io.mapsmessaging.rest.api.impl.server");
    endpoints.add("io.mapsmessaging.rest.api.impl.logging");
    endpoints.add("io.mapsmessaging.rest.api.impl.ml");
    try {
      final ResourceConfig resourceConfig = new ResourceConfig();
      resourceConfig.packages(false, endpoints.toArray(new String[0]));
      resourceConfig.property(ServerProperties.WADL_FEATURE_DISABLE, !config.isEnableWadlEndPoint());
      resourceConfig.register(SseFeature.class);
      // Register Gson providers
      resourceConfig.register(GsonMessageBodyReader.class);
      resourceConfig.register(GsonMessageBodyWriter.class);
      resourceConfig.register(EndpointIntrospector.class);
      if (config.isEnableAuthentication() && AuthManager.getInstance().isAuthenticationEnabled()) {
        resourceConfig.register(new AuthenticationFilter());
        AuthenticationContext.getInstance().setAccessControl(new RestAccessControl());
      }
      else{
        resourceConfig.register(new NoAuthenticationFilter());
      }
      resourceConfig.register(LoggingFilter.class);
      resourceConfig.register(CorsFilter.class);
      ServletContainer sc = new ServletContainer(resourceConfig);
      SSLContextConfigurator sslConfig = setupSSL();
      String protocol = "http";
      if(isSecure){
        protocol = "https";
      }
      baseUri = protocol+"://" + getHost() + ":" + getPort() + "/";

      httpServer = startHttpService(URI.create(baseUri), sc, sslConfig);
      httpServer.start();
      cleanupSchedule = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(
          SessionTracker::scan,
          inactiveTimeout,
          10_000,
          TimeUnit.MILLISECONDS
      );
      if(getHost().equals("0.0.0.0") || getHost().equals("::")){
        baseUri = protocol+"://"+MessageDaemon.getInstance().getHostname()+":"+getPort()+"/";
      }
    } catch (IOException e) {
      logger.log(REST_API_FAILURE, e);
    }
  }

  public HttpServer startHttpService(URI uri, Servlet servlet, SSLContextConfigurator sslConfig) throws IOException {
    String path = uri.getPath();
    if (path == null || path.isEmpty() || path.charAt(0) != '/') {
      throw new IllegalArgumentException("The URI path, of the URI " + uri + ", must be non-null, present and start with a '/'");
    }

    path = String.format("/%s", (UriComponent.decodePath(uri.getPath(), true).get(1)).toString());
    WebappContext context = new WebappContext("GrizzlyContext", path);
    ServletRegistration registration = context.addServlet(servlet.getClass().getName(), servlet);
    context.addListener(new SessionTracker());
    registration.addMapping("/*");
    HttpServer server;
    if (sslConfig != null) {
      SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(sslConfig, false, isClientCertRequired(), isClientCertWanted());
      server = GrizzlyHttpServerFactory.createHttpServer(uri, ((GrizzlyHttpContainer) null), true, sslEngineConfigurator, false);
    } else {
      server = GrizzlyHttpServerFactory.createHttpServer(uri, false);
    }
    ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.defaultConfig()
        .setCorePoolSize(config.getMinThreads()) // Minimum threads
        .setMaxPoolSize(config.getMaxThreads()) // Maximum threads
        .setQueueLimit(config.getThreadQueueLimit()); // Task queue limit

    TCPNIOTransport transport = server.getListener("grizzly").getTransport();
    transport.setSelectorRunnersCount(config.getSelectorThreads());
    transport.setWorkerThreadPoolConfig(threadPoolConfig);
    transport.setKernelThreadPoolConfig(threadPoolConfig);

    context.deploy(server);
    loadStatic(server);
    server.getServerConfiguration().setSessionTimeoutSeconds(BaseAuthenticationFilter.getMaxInactiveInterval()/1000);
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
            StaticHttpHandler staticHttpHandler = new CorsEnabledStaticHttpHandler(location+File.separator);
            staticHttpHandler.setFileCacheEnabled(true);
            server.getServerConfiguration().addHttpHandler(staticHttpHandler, uriMapping+"*" );
          }
        }
      }
    }
  }

  @Override
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName("RestAPI Caching");
    if(Constants.getCentralCache() instanceof RoleBasedCache){
      status.setStatus(Status.OK);
      status.setComment("Cached Items:"+Constants.getCentralCache().size());
    }
    else{
      status.setStatus(Status.DISABLED);
    }
    return status;
  }

}
