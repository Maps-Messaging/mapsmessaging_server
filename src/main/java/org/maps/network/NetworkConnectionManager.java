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
import org.maps.network.admin.EndPointConnectionHostJMX;
import org.maps.network.io.EndPointConnectionFactory;
import org.maps.network.io.connection.EndPointConnection;
import org.maps.network.io.impl.SelectorLoadManager;
import org.maps.utilities.configuration.ConfigurationManager;
import org.maps.utilities.configuration.ConfigurationProperties;
import org.maps.utilities.service.Service;
import org.maps.utilities.service.ServiceManager;

public class NetworkConnectionManager implements ServiceManager {

  private final Logger logger = LoggerFactory.getLogger(NetworkConnectionManager.class);
  private final SelectorLoadManager selectorLoadManager;
  private final ServiceLoader<EndPointConnectionFactory> endPointConnections;
  private final Map<Integer, ConfigurationProperties> networkConnectionProperties;
  private final Map<Integer, ConfigurationProperties> destinationMappingConfiguration;
  private final List<EndPointConnection> endPointConnectionList;
  private final Map<String, EndPointConnectionHostJMX> hostMapping;

  private final List<String> jmxParent;

  public NetworkConnectionManager(List<String> parent) throws IOException {
    logger.log(LogMessages.NETWORK_MANAGER_STARTUP);
    jmxParent = parent;
    networkConnectionProperties = ConfigurationManager.getInstance().getPropertiesList("NetworkConnectionManager");
    destinationMappingConfiguration = ConfigurationManager.getInstance().getPropertiesList("NetworkConnectionDetails");
    endPointConnections = ServiceLoader.load(EndPointConnectionFactory.class);
    logger.log(LogMessages.NETWORK_MANAGER_STARTUP_COMPLETE);
    selectorLoadManager = new SelectorLoadManager(10);
    endPointConnectionList = new ArrayList<>();
    hostMapping = new LinkedHashMap<>();
  }

  public void initialise() {
    for (Map.Entry<Integer, ConfigurationProperties> entry : networkConnectionProperties.entrySet()) {
      ConfigurationProperties properties = entry.getValue();
      String urlString = properties.getProperty("url");
      if (urlString != null) {
        EndPointURL endPointURL = new EndPointURL(urlString);
        List<ConfigurationProperties> destinationMappings = findMatchingDestinationMappings(properties.getProperty("name"));
        if(!destinationMappings.isEmpty()) {
          for (EndPointConnectionFactory endPointConnectionFactory : endPointConnections) {
            if (endPointConnectionFactory.getName().equals(endPointURL.getProtocol())) {
              EndPointURL url = new EndPointURL(properties.getProperty("url"));
              EndPointConnectionHostJMX hostJMXBean = hostMapping.computeIfAbsent(url.host, k -> new EndPointConnectionHostJMX(jmxParent, url.host));
              endPointConnectionList.add(new EndPointConnection(url, properties, destinationMappings, endPointConnectionFactory, selectorLoadManager, hostJMXBean));
            }
          }
        }
        else{
          System.err.println("Ignoring config for "+properties);
        }
      }
    }
  }

  private List<ConfigurationProperties> findMatchingDestinationMappings(String name){
    List<ConfigurationProperties> response = new ArrayList<>();
    for (ConfigurationProperties properties : destinationMappingConfiguration.values()) {
      String lookup = properties.getProperty("connection_name");
      if (lookup != null && lookup.equals(name)) {
        response.add(properties);
      }
    }
    return response;
  }

  public SelectorLoadManager getSelectorLoadManager() {
    return selectorLoadManager;
  }

  public void start() {
    logger.log(LogMessages.NETWORK_MANAGER_START_ALL);
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
