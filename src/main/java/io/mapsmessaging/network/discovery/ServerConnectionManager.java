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

package io.mapsmessaging.network.discovery;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.Agent;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

public class ServerConnectionManager implements ServiceListener, Agent {

  private final Logger logger = LoggerFactory.getLogger(ServerConnectionManager.class);

  public ServerConnectionManager(){
  }

  @Override
  public void serviceAdded(ServiceEvent serviceEvent) {
  }

  @Override
  public void serviceRemoved(ServiceEvent serviceEvent) {
    if(!serviceEvent.getName().startsWith(MessageDaemon.getInstance().getId())){ // Ignore local
      for(String host:serviceEvent.getInfo().getHostAddresses()){
        logger.log(ServerLogMessages.DISCOVERY_REMOVED_REMOTE_SERVER, serviceEvent.getName(), host+":"+serviceEvent.getInfo().getPort(), serviceEvent.getInfo().getApplication());
      }
    }

  }

  @Override
  public void serviceResolved(ServiceEvent serviceEvent) {
    if(!serviceEvent.getName().startsWith(MessageDaemon.getInstance().getId())){ // Ignore local
      for(String host:serviceEvent.getInfo().getHostAddresses()){
        logger.log(ServerLogMessages.DISCOVERY_RESOLVED_REMOTE_SERVER, serviceEvent.getName(), host+":"+serviceEvent.getInfo().getPort(), serviceEvent.getInfo().getApplication());
      }
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
  }

  @Override
  public void stop() {

  }
}
