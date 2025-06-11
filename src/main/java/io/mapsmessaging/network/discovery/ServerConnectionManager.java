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

package io.mapsmessaging.network.discovery;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.discovery.services.RemoteServers;
import io.mapsmessaging.utilities.Agent;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServerConnectionManager implements ServiceListener, Agent {

  private final Logger logger = LoggerFactory.getLogger(ServerConnectionManager.class);
  private final Map<String, RemoteServers> serviceInfoMap;

  public ServerConnectionManager(){
    serviceInfoMap = new LinkedHashMap<>();
  }

  @Override
  public void serviceAdded(ServiceEvent serviceEvent) {
    // we don't need to worry about this call back
  }

  public List<RemoteServers> getServers() {
    return new ArrayList<>(serviceInfoMap.values());
  }

  @Override
  public void serviceRemoved(ServiceEvent serviceEvent) {
    if(!serviceEvent.getName().startsWith(MessageDaemon.getInstance().getId())){ // Ignore local
      if(serviceEvent.getInfo().getName() == null){
        return;
      }
      
      MapsServiceInfo mapsServiceInfo = new MapsServiceInfo(serviceEvent.getInfo());
      RemoteServers server = serviceInfoMap.get(mapsServiceInfo.getServerName());
      if(server != null){
        logger.log(ServerLogMessages.DISCOVERY_REMOVED_REMOTE_SERVER, serviceEvent.getName(), server.getServerName()+":"+serviceEvent.getInfo().getPort(), serviceEvent.getInfo().getApplication());
        server.remove(mapsServiceInfo);
        if(server.getServices().isEmpty()){
          serviceInfoMap.remove(mapsServiceInfo.getServerName());
        }
      }
    }
  }

  @Override
  public synchronized void serviceResolved(ServiceEvent serviceEvent) {
    if(!serviceEvent.getName().startsWith(MessageDaemon.getInstance().getId()) && serviceEvent.getInfo().hasData()){
      if(serviceEvent.getInfo().getPropertyString("server name") == null){
        return;
      }
      MapsServiceInfo mapsServiceInfo = new MapsServiceInfo(serviceEvent.getInfo());
      String name = mapsServiceInfo.getServerName();
      RemoteServers remoteServer = serviceInfoMap.get(name);
      if(remoteServer == null){
        remoteServer = new RemoteServers(mapsServiceInfo);
        serviceInfoMap.put(name, remoteServer);
      }
      else{
        remoteServer.update(mapsServiceInfo);
      }
      logger.log(ServerLogMessages.DISCOVERY_RESOLVED_REMOTE_SERVER, name, mapsServiceInfo.getName() + ":" + serviceEvent.getInfo().getPort(), serviceEvent.getInfo().getApplication());
    }
  }

  @Override
  public String getName() {
    return "Server Connection Manager";
  }

  @Override
  public String getDescription() {
    return "Listens for new maps servers via mDNS";
  }

  @Override
  public void start() {
    DiscoveryManager discoveryManager = MessageDaemon.getInstance().getSubSystemManager().getDiscoveryManager();
    discoveryManager.registerListener("_maps._tcp.local.", this);
    discoveryManager.registerListener("_mqtt._tcp.local.", this);
    discoveryManager.registerListener("_amqp._tcp.local.", this);
    discoveryManager.registerListener("_stomp._tcp.local.", this);
    discoveryManager.registerListener("_coap._udp.local.", this);
    discoveryManager.registerListener("_mqtt-sn._udp.local.", this);
  }

  @Override
  public void stop() {
  }


  @Override
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName(getName());
    if (MessageDaemon.getInstance().getSubSystemManager().getDiscoveryManager().isEnabled()) {
      status.setComment("Discovered: " + serviceInfoMap.size());
      status.setStatus(Status.OK);
    }
    else{
      status.setStatus(Status.DISABLED);
    }
    return status;
  }

}
