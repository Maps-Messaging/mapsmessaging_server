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

package io.mapsmessaging.routing;

import static io.mapsmessaging.logging.ServerLogMessages.ROUTING_SHUTDOWN;
import static io.mapsmessaging.logging.ServerLogMessages.ROUTING_STARTUP;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.discovery.DiscoveryManager;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.impl.JmDNSImpl;

public class RoutingManager implements Agent, ServiceListener {

  private final Logger logger = LoggerFactory.getLogger(RoutingManager.class);

  private final ConfigurationProperties properties;
  private final boolean enabled;
  private final boolean autoConfig;

  public RoutingManager() {
    properties = ConfigurationManager.getInstance().getProperties("routing");
    enabled = properties.getBooleanProperty("enabled", false);
    autoConfig = properties.getBooleanProperty("autoDiscovery", false);
  }

  @Override
  public String getName() {
    return "Event Routing Manager";
  }

  @Override
  public String getDescription() {
    return "Monitors remote server status and manages event routing rules for this server";
  }

  public void start() {
    if (enabled) {
      logger.log(ROUTING_STARTUP);
      if(autoConfig){
        DiscoveryManager discoveryManager = MessageDaemon.getInstance().getDiscoveryManager();
        if(discoveryManager.isEnabled()) {
          // Register listener for map server notification
          discoveryManager.registerListener("_maps._tcp.local.", this);
        }
      }
    }
  }

  public void stop() {
    logger.log(ROUTING_SHUTDOWN);
    if(autoConfig) {
      DiscoveryManager discoveryManager = MessageDaemon.getInstance().getDiscoveryManager();
      if(discoveryManager.isEnabled()) {
        discoveryManager.removeListener("_maps._tcp.local.", this);
      }
    }
  }

  @Override
  public void serviceAdded(ServiceEvent serviceEvent) {
    if(!isLocal(serviceEvent)){
      System.err.println("Added service:: "+serviceEvent.getName()+" "+serviceEvent.getInfo());
    }
  }

  @Override
  public void serviceRemoved(ServiceEvent serviceEvent) {
    if(!isLocal(serviceEvent)) {
      System.err.println("Removed service:: "+serviceEvent.getName()+" "+serviceEvent.getInfo());
    }
  }

  @Override
  public void serviceResolved(ServiceEvent serviceEvent) {
    if(!isLocal(serviceEvent)) {
      boolean restSupport = serviceEvent.getInfo().getPropertyString("restApi").trim().toLowerCase().equals("true");
      if(restSupport) {
        String protocol = serviceEvent.getInfo().getPropertyString("protocol");
        String host = serviceEvent.getInfo().getHostAddresses()[0];
        boolean schemaSupport = serviceEvent.getInfo().getPropertyString("schema support").trim().toLowerCase().equals("true");
        RemoteServerManager remoteManager = new RemoteServerManager(protocol+"://"+host+":"+serviceEvent.getInfo().getPort(), schemaSupport);
      }
    }
  }

  private boolean isLocal(ServiceEvent serviceEvent){
    Object source = serviceEvent.getSource();
    if(source instanceof JmDNSImpl){
      JmDNSImpl impl = (JmDNSImpl) source;
      return (impl.getLocalHost().getName().toLowerCase().startsWith(serviceEvent.getInfo().getName().toLowerCase()));
    }
    return false;
  }
}