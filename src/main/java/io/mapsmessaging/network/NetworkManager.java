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

package io.mapsmessaging.network;

import io.mapsmessaging.config.NetworkManagerConfig;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.EndPointManager.STATE;
import io.mapsmessaging.network.admin.NetworkManagerJMX;
import io.mapsmessaging.network.io.EndPointServerFactory;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkManager implements ServiceManager, Agent {

  private final Logger logger = LoggerFactory.getLogger(NetworkManager.class);
  private final List<EndPointServerFactory> endPointServers;
  private final LinkedHashMap<String, EndPointManager> endPointManagers;
  private final NetworkManagerJMX bean;
  private final NetworkManagerConfig config;

  public NetworkManager(FeatureManager featureManager) {
    logger.log(ServerLogMessages.NETWORK_MANAGER_STARTUP);
    endPointManagers = new LinkedHashMap<>();
    config = NetworkManagerConfig.getInstance();
    logger.log(ServerLogMessages.NETWORK_MANAGER_LOAD_PROPERTIES);
    ServiceLoader<EndPointServerFactory> endPointServerLoad = ServiceLoader.load(EndPointServerFactory.class);
    endPointServers = new CopyOnWriteArrayList<>();
    for(EndPointServerFactory endPointConnectionFactory:endPointServerLoad){
      String name = endPointConnectionFactory.getName().toLowerCase();
      if(name.equals("noop") || featureManager.isEnabled("network."+name)){
        endPointServers.add(endPointConnectionFactory);
      }
    }
    logger.log(ServerLogMessages.NETWORK_MANAGER_STARTUP_COMPLETE);

    System.setProperty("java.net.preferIPv6Addresses", ""+config.isPreferIpV6Addresses());
    bean = new NetworkManagerJMX(this);
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
    for (EndPointServerConfigDTO endPointServerConfig : config.getEndPointServerConfigList()) {
      EndPointURL endPointURL = EndPointURLFactory.getInstance().create(endPointServerConfig.getUrl());
      initialiseInstance(endPointURL, endPointServerConfig);
    }
    startAll();
  }


  @Override
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName(getName());
    status.setComment("");
    if(endPointManagers.isEmpty()){
      status.setStatus(Status.WARN);
      status.setComment("No bound end points");
      return status;
    }
    status.setStatus(Status.OK);

    AtomicInteger stopped = new AtomicInteger(0);
    endPointManagers.entrySet().forEach(entry -> {
      EndPointManager endPointManager = entry.getValue();
      if (endPointManager.getState() == STATE.STOPPED) {
        stopped.incrementAndGet();
      }
    });
    if(stopped.get() != 0){
      if(stopped.get() == endPointManagers.size()){
        status.setStatus(Status.STOPPED);
        status.setComment("All end points are stopped");
      }
      else{
        status.setStatus(Status.WARN);
        status.setComment("Some end points are stopped");
      }
      return status;
    }

    stopped.set(0);
    endPointManagers.entrySet().forEach(entry -> {
      EndPointManager endPointManager = entry.getValue();
      if (endPointManager.getState() == STATE.PAUSED) {
        stopped.incrementAndGet();
      }
    });
    if(stopped.get() != 0){
      if(stopped.get() == endPointManagers.size()){
        status.setStatus(Status.PAUSED);
        status.setComment("All end points are stopped");
      }
      else{
        status.setStatus(Status.WARN);
        status.setComment("Some end points are paused");
      }
      return status;
    }

    return status;
  }


  // We are constructing end points which open a resource, we need this resource to remain open
  // we add it to a list that manages the close
  @SuppressWarnings("java:S2095")
  private void initialiseInstance(EndPointURL endPointURL, EndPointServerConfigDTO endPointServerConfig) {
    for (EndPointServerFactory endPointServerFactory : endPointServers) {
      if (endPointServerFactory.getName().equals(endPointURL.getProtocol())) {
        if (endPointServerFactory.active()) {
          try {
            EndPointManager endPointManager = new EndPointManager(endPointURL, endPointServerFactory, endPointServerConfig, bean);
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
    endPointManagers.forEach((key, endPointManager) -> {
      try {
        if (endPointManager.getState() == STATE.STOPPED) {
          endPointManager.start();
        }
      } catch (IOException e) {
        logger.log(ServerLogMessages.NETWORK_MANAGER_START_FAILED, e, key);
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
    List<Service> service = new ArrayList<>(endPointServers);
    return service.listIterator();
  }
}
