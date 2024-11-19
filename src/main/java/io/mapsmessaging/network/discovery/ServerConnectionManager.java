/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.network.discovery;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.discovery.services.RemoteServers;
import io.mapsmessaging.utilities.Agent;
import java.util.*;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

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
    MessageDaemon.getInstance().getDiscoveryManager().registerListener("_maps._tcp.local.", this);
    MessageDaemon.getInstance().getDiscoveryManager().registerListener("_mqtt._tcp.local.", this);
    MessageDaemon.getInstance().getDiscoveryManager().registerListener("_amqp._tcp.local.", this);
    MessageDaemon.getInstance().getDiscoveryManager().registerListener("_stomp._tcp.local.", this);
    MessageDaemon.getInstance().getDiscoveryManager().registerListener("_coap._udp.local.", this);
    MessageDaemon.getInstance().getDiscoveryManager().registerListener("_mqtt-sn._udp.local.", this);
  }

  @Override
  public void stop() {
  }


}
