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

package io.mapsmessaging.routing;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.config.RoutingManagerConfig;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.discovery.DiscoveryManager;
import io.mapsmessaging.utilities.Agent;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.impl.JmDNSImpl;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.mapsmessaging.logging.ServerLogMessages.ROUTING_SHUTDOWN;
import static io.mapsmessaging.logging.ServerLogMessages.ROUTING_STARTUP;

public class RoutingManager implements Agent, ServiceListener {

  private final Logger logger = LoggerFactory.getLogger(RoutingManager.class);

  private final RoutingManagerConfig config;
  private final Map<String, RemoteServerManager> remoteServers;

  public RoutingManager() {
    config = RoutingManagerConfig.getInstance();
    remoteServers = new LinkedHashMap<>();
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
    if (config.isEnabled()) {
      logger.log(ROUTING_STARTUP);
      if (config.isAutoDiscovery()) {
        DiscoveryManager discoveryManager = MessageDaemon.getInstance().getSubSystemManager().getDiscoveryManager();
        if(discoveryManager.isEnabled()) {
          // Register listener for map server notification
          discoveryManager.registerListener("_maps._tcp.local.", this);
        }
      }
    }
  }

  public void stop() {
    logger.log(ROUTING_SHUTDOWN);
    if(config.isAutoDiscovery()) {
      DiscoveryManager discoveryManager = MessageDaemon.getInstance().getSubSystemManager().getDiscoveryManager();
      if(discoveryManager.isEnabled()) {
        discoveryManager.removeListener("_maps._tcp.local.", this);
      }
      for(RemoteServerManager remoteServerManager: remoteServers.values()){
        remoteServerManager.stop();
      }
    }
  }

  @Override
  public void serviceAdded(ServiceEvent serviceEvent) {
    if(isNotLocal(serviceEvent)){
      String key = buildKey(serviceEvent);
      if(remoteServers.containsKey(key)) {
        remoteServers.get(key).resume();
      }
    }
  }

  @Override
  public void serviceRemoved(ServiceEvent serviceEvent) {
    if(isNotLocal(serviceEvent)) {
      String key = buildKey(serviceEvent);
      RemoteServerManager remoteServerManager = remoteServers.get(key);
      if(remoteServerManager != null){
        remoteServerManager.pause();
      }
    }
  }

  @Override
  public void serviceResolved(ServiceEvent serviceEvent) {
    if(isNotLocal(serviceEvent)) {
      boolean restSupport = serviceEvent.getInfo().getPropertyString("restApi").trim().equalsIgnoreCase("true");
      if(restSupport) {
        boolean schemaSupport = serviceEvent.getInfo().getPropertyString("schema support").trim().equalsIgnoreCase("true");
        String key = buildKey(serviceEvent);
        if(!remoteServers.containsKey(key)){
          remoteServers.put(key, new RemoteServerManager(key, schemaSupport));
        }
        else{
          remoteServers.get(key).resume();
        }
      }
    }
  }

  private String buildKey(ServiceEvent serviceEvent){
    String protocol = serviceEvent.getInfo().getPropertyString("protocol");
    String host = serviceEvent.getInfo().getHostAddresses()[0];
    return protocol+"://"+host+":"+serviceEvent.getInfo().getPort();
  }

  private boolean isNotLocal(ServiceEvent serviceEvent){
    Object source = serviceEvent.getSource();
    if(source instanceof JmDNSImpl){
      JmDNSImpl impl = (JmDNSImpl) source;
      return !(impl.getLocalHost().getName().toLowerCase().startsWith(serviceEvent.getInfo().getName().toLowerCase()));
    }
    return true;
  }

  @Override
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName(getName());
    status.setComment("Not Active");
    status.setStatus(Status.DISABLED);
    return status;
  }

}