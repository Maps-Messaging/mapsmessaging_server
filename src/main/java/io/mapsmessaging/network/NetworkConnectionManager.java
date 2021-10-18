/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.admin.EndPointConnectionHostJMX;
import io.mapsmessaging.network.io.EndPointConnectionFactory;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public class NetworkConnectionManager implements ServiceManager {

  private final Logger logger = LoggerFactory.getLogger(NetworkConnectionManager.class);
  private final SelectorLoadManager selectorLoadManager;
  private final ServiceLoader<EndPointConnectionFactory> endPointConnections;
  private final List<EndPointConnection> endPointConnectionList;
  private final Map<String, EndPointConnectionHostJMX> hostMapping;
  private final List<ConfigurationProperties> connectionConfiguration;

  private final List<String> jmxParent;

  public NetworkConnectionManager(List<String> parent) throws IOException {
    logger.log(ServerLogMessages.NETWORK_MANAGER_STARTUP);
    jmxParent = parent;
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
    selectorLoadManager = new SelectorLoadManager(10);
    endPointConnectionList = new ArrayList<>();
    hostMapping = new LinkedHashMap<>();
  }

  public void initialise() {
    for (ConfigurationProperties properties : connectionConfiguration) {
      String urlString = properties.getProperty("url");
      if(urlString == null){
        urlString = "noop://localhost/";
        properties.put("protocol", "loop");
      }

      EndPointURL endPointURL = new EndPointURL(urlString);
      List<ConfigurationProperties> destinationMappings = new ArrayList<>();
      Object linkReference = properties.get("links");
      if(linkReference instanceof ConfigurationProperties){
        destinationMappings.add((ConfigurationProperties) linkReference);
      }
      else if(linkReference instanceof List){
        destinationMappings.addAll((List<ConfigurationProperties>)linkReference);
      }
      if (!destinationMappings.isEmpty()) {
        for (EndPointConnectionFactory endPointConnectionFactory : endPointConnections) {
          if (endPointConnectionFactory.getName().equals(endPointURL.getProtocol())) {
            EndPointConnectionHostJMX hostJMXBean = hostMapping.computeIfAbsent(endPointURL.host, k -> new EndPointConnectionHostJMX(jmxParent, endPointURL.host));
            endPointConnectionList.add(new EndPointConnection(endPointURL, properties, destinationMappings, endPointConnectionFactory, selectorLoadManager, hostJMXBean));
          }
        }
      }
    }
  }

  public SelectorLoadManager getSelectorLoadManager() {
    return selectorLoadManager;
  }

  public void start() {
    logger.log(ServerLogMessages.NETWORK_MANAGER_START_ALL);
    for(EndPointConnection endPointConnection : endPointConnectionList){
      endPointConnection.start();
    }
  }

  public void stop() {
    for(EndPointConnection endPointConnection : endPointConnectionList){
      endPointConnection.stop();
    }
  }

  @Override
  public Iterator<Service> getServices() {
    List<Service> service = new ArrayList<>();
    for(EndPointConnectionFactory endPointConnectionFactory:endPointConnections){
      service.add(endPointConnectionFactory);
    }
    return service.listIterator();
  }

  public List<String> getJMXParent() {
    return jmxParent;
  }
}
