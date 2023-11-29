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

package io.mapsmessaging.network;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.EndPointManager.STATE;
import io.mapsmessaging.network.admin.NetworkManagerJMX;
import io.mapsmessaging.network.io.EndPointServerFactory;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;

import java.io.IOException;
import java.util.*;

public class NetworkManager implements ServiceManager, Agent {

  private final Logger logger = LoggerFactory.getLogger(NetworkManager.class);
  private final ServiceLoader<EndPointServerFactory> endPointServers;
  private final LinkedHashMap<String, EndPointManager> endPointManagers;
  private final NetworkManagerJMX bean;
  private final List<ConfigurationProperties> adapters;

  public NetworkManager(List<String> parent) {
    logger.log(ServerLogMessages.NETWORK_MANAGER_STARTUP);
    endPointManagers = new LinkedHashMap<>();

    ConfigurationProperties properties = ConfigurationManager.getInstance().getProperties("NetworkManager");
    Object obj = properties.get("data");
    adapters = new ArrayList<>();
    if (obj instanceof List) {
      adapters.addAll((List<ConfigurationProperties>) obj);
    } else if (obj instanceof ConfigurationProperties) {
      adapters.add((ConfigurationProperties) obj);
    }
    logger.log(ServerLogMessages.NETWORK_MANAGER_LOAD_PROPERTIES);
    endPointServers = ServiceLoader.load(EndPointServerFactory.class);
    logger.log(ServerLogMessages.NETWORK_MANAGER_STARTUP_COMPLETE);
    if (properties.getBooleanProperty("preferIPv6Addresses", true)) {
      System.setProperty("java.net.preferIPv6Addresses", "true");
    }

    bean = new NetworkManagerJMX(parent, this);
  }

  @Override
  public String getName() {
    return "Network Manager";
  }

  @Override
  public String getDescription() {
    return "Manages all of the adapters/protocols used by the server";
  }

  public void start() {
    initialise();
  }

  public void stop() {
    stopAll();
  }

  public void initialise() {
    for (ConfigurationProperties configurationProperties : adapters) {
      NetworkConfig networkConfig = new NetworkConfig(configurationProperties);
      EndPointURL endPointURL = EndPointURLFactory.getInstance().create(networkConfig.getUrl());
      initialiseInstance(endPointURL, networkConfig);
    }
    startAll();
  }

  // We are constructing end points which open a resource, we need this resource to remain open
  // we add it to a list that manages the close
  @SuppressWarnings("java:S2095")
  private void initialiseInstance(EndPointURL endPointURL, NetworkConfig networkConfig) {
    for (EndPointServerFactory endPointServerFactory : endPointServers) {
      if (endPointServerFactory.getName().equals(endPointURL.getProtocol())) {
        if (endPointServerFactory.active()) {
          try {
            EndPointManager endPointManager = new EndPointManager(endPointURL, endPointServerFactory, networkConfig, bean);
            endPointManagers.put(endPointURL.toString(), endPointManager);
          } catch (IOException | RuntimeException iox) {
            logger.log(ServerLogMessages.NETWORK_MANAGER_START_FAILURE, iox, endPointURL.toString());
          }
        } else {
          logger.log(ServerLogMessages.NETWORK_MANAGER_DEVICE_NOT_LOADED, endPointServerFactory.getName());
        }
      }
    }
  }

  public  List<EndPointManager> getAll(){
    List<EndPointManager> response = new ArrayList<>();
    for (Map.Entry<String, EndPointManager> entry : endPointManagers.entrySet()) {
        response.add(entry.getValue());
    }
    return response;
  }

  public void startAll() {
    logger.log(ServerLogMessages.NETWORK_MANAGER_START_ALL);
    endPointManagers.entrySet().parallelStream().forEach(entry -> {
      try {
        EndPointManager endPointManager = entry.getValue();
        if (endPointManager.getState() == STATE.STOPPED) {
          entry.getValue().start();
        }
      } catch (IOException e) {
        logger.log(ServerLogMessages.NETWORK_MANAGER_START_FAILED, e, entry.getKey());
      }
    });
  }

  public void stopAll() {
    logger.log(ServerLogMessages.NETWORK_MANAGER_STOP_ALL);
    endPointManagers.entrySet().parallelStream().forEach(entry -> {
      try {
        EndPointManager endPointManager = entry.getValue();
        if (endPointManager.getState() != STATE.STOPPED) {
          endPointManager.close();
        }
      } catch (IOException e) {
        logger.log(ServerLogMessages.NETWORK_MANAGER_STOP_FAILED, e, entry.getKey());
      }
    });
  }

  public void pauseAll() {
    logger.log(ServerLogMessages.NETWORK_MANAGER_PAUSE_ALL);
    for (Map.Entry<String, EndPointManager> entry : endPointManagers.entrySet()) {
      try {
        EndPointManager endPointManager = entry.getValue();
        if (endPointManager.getState() != STATE.STOPPED
            && endPointManager.getState() != STATE.PAUSED) {
          endPointManager.pause();
        }
      } catch (IOException e) {
        logger.log(ServerLogMessages.NETWORK_MANAGER_PAUSE_FAILED, e, entry.getKey());
      }
    }
  }

  public void resumeAll() {
    logger.log(ServerLogMessages.NETWORK_MANAGER_RESUME_ALL);
    for (Map.Entry<String, EndPointManager> entry : endPointManagers.entrySet()) {
      try {
        EndPointManager endPointManager = entry.getValue();
        if (endPointManager.getState() != STATE.STOPPED
            && endPointManager.getState() == STATE.PAUSED) {
          endPointManager.resume();
        }
      } catch (IOException e) {
        logger.log(ServerLogMessages.NETWORK_MANAGER_RESUME_FAILED, e, entry.getKey());
      }
    }
  }

  public int size() {
    return endPointManagers.size();
  }

  @Override
  public Iterator<Service> getServices() {
    List<Service> service = new ArrayList<>();
    for (EndPointServerFactory endPointServer : endPointServers) {
      service.add(endPointServer);
    }
    return service.listIterator();
  }
}
