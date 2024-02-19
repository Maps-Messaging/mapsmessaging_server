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

package io.mapsmessaging.network;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.admin.EndPointConnectionHostJMX;
import io.mapsmessaging.network.io.EndPointConnectionFactory;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;
import lombok.Getter;

import java.io.IOException;
import java.util.*;

public class NetworkConnectionManager implements ServiceManager, Agent {

  @Getter
  private final SelectorLoadManager selectorLoadManager;

  private final Logger logger = LoggerFactory.getLogger(NetworkConnectionManager.class);
  private final ServiceLoader<EndPointConnectionFactory> endPointConnections;
  private final List<EndPointConnection> endPointConnectionList;
  private final Map<String, EndPointConnectionHostJMX> hostMapping;
  private final List<ConfigurationProperties> connectionConfiguration;

  public NetworkConnectionManager() throws IOException {
    logger.log(ServerLogMessages.NETWORK_MANAGER_STARTUP);
    ConfigurationProperties networkConnectionProperties = ConfigurationManager.getInstance().getProperties("NetworkConnectionManager");
    connectionConfiguration = new ArrayList<>();
    Object rootObj = networkConnectionProperties.get("data");
    if (rootObj instanceof List) {
      connectionConfiguration.addAll((List<ConfigurationProperties>) rootObj);
    } else if (rootObj instanceof ConfigurationProperties) {
      connectionConfiguration.add((ConfigurationProperties) rootObj);
    }
    endPointConnections = ServiceLoader.load(EndPointConnectionFactory.class);
    logger.log(ServerLogMessages.NETWORK_MANAGER_STARTUP_COMPLETE);
    selectorLoadManager = new SelectorLoadManager(10,networkConnectionProperties.getProperty("name", "Network Connection") );
    endPointConnectionList = new ArrayList<>();
    hostMapping = new LinkedHashMap<>();
  }

  public void initialise() {
    for (ConfigurationProperties properties : connectionConfiguration) {
      String urlString = properties.getProperty("url");
      if (urlString == null) {
        urlString = "noop://localhost/";
        properties.put("protocol", "loop");
      }

      EndPointURL endPointURL = new EndPointURL(urlString);
      List<ConfigurationProperties> destinationMappings = new ArrayList<>();
      Object linkReference = properties.get("links");
      if (linkReference instanceof ConfigurationProperties) {
        destinationMappings.add((ConfigurationProperties) linkReference);
      } else if (linkReference instanceof List) {
        destinationMappings.addAll((List<ConfigurationProperties>) linkReference);
      }
      if (!destinationMappings.isEmpty()) {
        processEndPoint(endPointURL, properties,destinationMappings );
      }
    }
  }

  private void processEndPoint(EndPointURL endPointURL, ConfigurationProperties properties, List<ConfigurationProperties> destinationMappings){
    for (EndPointConnectionFactory endPointConnectionFactory : endPointConnections) {
      if (endPointConnectionFactory.getName().equals(endPointURL.getProtocol())) {
        EndPointConnectionHostJMX hostJMXBean = null;
        List<String> jmxList = MessageDaemon.getInstance().getTypePath();
        if (!jmxList.isEmpty()) {
          hostJMXBean = hostMapping.computeIfAbsent(endPointURL.host, k -> new EndPointConnectionHostJMX(jmxList, endPointURL.host));
        }
        endPointConnectionList.add(new EndPointConnection(endPointURL, properties, destinationMappings, endPointConnectionFactory, selectorLoadManager, hostJMXBean));
      }
    }
  }

  @Override
  public String getName() {
    return "Network Connection Manager";
  }

  @Override
  public String getDescription() {
    return "Orchestrates remote connections to other messaging servers";
  }

  public void start() {
    initialise();
    logger.log(ServerLogMessages.NETWORK_MANAGER_START_ALL);
    for (EndPointConnection endPointConnection : endPointConnectionList) {
      endPointConnection.start();
    }
  }

  public void stop() {
    for (EndPointConnection endPointConnection : endPointConnectionList) {
      endPointConnection.stop();
    }
  }

  @Override
  public Iterator<Service> getServices() {
    List<Service> service = new ArrayList<>();
    for (EndPointConnectionFactory endPointConnectionFactory : endPointConnections) {
      service.add(endPointConnectionFactory);
    }
    return service.listIterator();
  }
}
