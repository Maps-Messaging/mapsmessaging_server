/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.network.EndPointManager.STATE;
import org.maps.network.admin.NetworkManagerJMX;
import org.maps.network.io.EndPointServerFactory;
import org.maps.utilities.configuration.ConfigurationProperties;
import org.maps.utilities.configuration.ConfigurationManager;
import org.maps.utilities.service.Service;
import org.maps.utilities.service.ServiceManager;

public class NetworkManager implements ServiceManager {

  private final Logger logger = LoggerFactory.getLogger(NetworkManager.class);
  private final ServiceLoader<EndPointServerFactory> endPointServers;
  private final LinkedHashMap<String, EndPointManager> endPointManagers;
  private final ConfigurationProperties properties;
  private final NetworkManagerJMX bean;
  private final List<ConfigurationProperties> adapters;

  public NetworkManager(List<String> parent) {
    logger.log(LogMessages.NETWORK_MANAGER_STARTUP);
    endPointManagers = new LinkedHashMap<>();

    properties = ConfigurationManager.getInstance().getProperties("NetworkManager");
    Object obj =  properties.get("data");
    adapters = new ArrayList<>();
    if(obj instanceof List){
      adapters.addAll((List<ConfigurationProperties>) obj);
    }
    else if(obj instanceof ConfigurationProperties){
      adapters.add((ConfigurationProperties) obj);
    }
    logger.log(LogMessages.NETWORK_MANAGER_LOAD_PROPERTIES);
    endPointServers = ServiceLoader.load(EndPointServerFactory.class);
    logger.log(LogMessages.NETWORK_MANAGER_STARTUP_COMPLETE);

    bean = new NetworkManagerJMX(parent, this);
  }

  public void initialise() {
    for (ConfigurationProperties configurationProperties : adapters) {
      NetworkConfig networkConfig = new NetworkConfig(configurationProperties);
      EndPointURL endPointURL = EndPointURLFactory.getInstance().create(networkConfig.getUrl());
      initialiseInstance(endPointURL, networkConfig);
    }
  }

  private void initialiseInstance( EndPointURL endPointURL, NetworkConfig networkConfig){
    for (EndPointServerFactory endPointServerFactory : endPointServers) {
      if (endPointServerFactory.getName().equals(endPointURL.getProtocol())) {
        if(endPointServerFactory.active()) {
          try {
            EndPointManager endPointManager = new EndPointManager(endPointURL, endPointServerFactory, networkConfig, bean);
            endPointManagers.put(endPointURL.toString(), endPointManager);
          } catch (IOException iox) {
            logger.log(LogMessages.NETWORK_MANAGER_START_FAILURE, iox, endPointURL.toString());
          }
        }
        else{
          logger.log(LogMessages.NETWORK_MANAGER_DEVICE_NOT_LOADED, endPointServerFactory.getName());
        }
      }
    }
  }

  public void startAll() {
    logger.log(LogMessages.NETWORK_MANAGER_START_ALL);
    for (Map.Entry<String, EndPointManager> entry : endPointManagers.entrySet()) {
      try {
        EndPointManager endPointManager = entry.getValue();
        if (endPointManager.getState() == STATE.CLOSED) {
          entry.getValue().start();
        }
      } catch (IOException e) {
        logger.log(LogMessages.NETWORK_MANAGER_START_FAILED, e, entry.getKey());
      }
    }
  }

  public void stopAll() {
    logger.log(LogMessages.NETWORK_MANAGER_STOP_ALL);
    for (Map.Entry<String, EndPointManager> entry : endPointManagers.entrySet()) {
      try {
        EndPointManager endPointManager = entry.getValue();
        if (endPointManager.getState() != STATE.CLOSED) {
          endPointManager.close();
        }
      } catch (IOException e) {
        logger.log(LogMessages.NETWORK_MANAGER_STOP_FAILED, e, entry.getKey());
      }
    }
  }

  public void pauseAll() {
    logger.log(LogMessages.NETWORK_MANAGER_PAUSE_ALL);
    for (Map.Entry<String, EndPointManager> entry : endPointManagers.entrySet()) {
      try {
        EndPointManager endPointManager = entry.getValue();
        if (endPointManager.getState() != STATE.CLOSED
            && endPointManager.getState() != STATE.PAUSED) {
          endPointManager.pause();
        }
      } catch (IOException e) {
        logger.log(LogMessages.NETWORK_MANAGER_PAUSE_FAILED, e, entry.getKey());
      }
    }
  }

  public void resumeAll() {
    logger.log(LogMessages.NETWORK_MANAGER_RESUME_ALL);
    for (Map.Entry<String, EndPointManager> entry : endPointManagers.entrySet()) {
      try {
        EndPointManager endPointManager = entry.getValue();
        if (endPointManager.getState() != STATE.CLOSED
            && endPointManager.getState() == STATE.PAUSED) {
          endPointManager.resume();
        }
      } catch (IOException e) {
        logger.log(LogMessages.NETWORK_MANAGER_RESUME_FAILED, e, entry.getKey());
      }
    }
  }

  public int size() {
    return endPointManagers.size();
  }

  @Override
  public Iterator<Service> getServices() {
    List<Service> service = new ArrayList<>();
    for(EndPointServerFactory endPointServer:endPointServers){
      service.add(endPointServer);
    }
    return service.listIterator();
  }
}
