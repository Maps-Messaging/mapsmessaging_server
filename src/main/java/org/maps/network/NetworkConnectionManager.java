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
  private final Map<Integer, ConfigurationProperties> properties;
  private final List<EndPointConnection> endPointConnectionList;
  private final Map<String, EndPointConnectionHostJMX> hostMapping;

  private final List<String> jmxParent;

  public NetworkConnectionManager(List<String> parent) throws IOException {
    logger.log(LogMessages.NETWORK_MANAGER_STARTUP);
    jmxParent = parent;
    properties = ConfigurationManager.getInstance().getPropertiesList("NetworkConnectionManager");
    endPointConnections = ServiceLoader.load(EndPointConnectionFactory.class);
    logger.log(LogMessages.NETWORK_MANAGER_STARTUP_COMPLETE);
    selectorLoadManager = new SelectorLoadManager(10);
    endPointConnectionList = new ArrayList<>();
    hostMapping = new LinkedHashMap<>();
  }

  public void initialise() {
    for (Map.Entry<Integer, ConfigurationProperties> entry : properties.entrySet()) {
      ConfigurationProperties properties = entry.getValue();
      EndPointURL endPointURL = new EndPointURL(properties.getProperty("url"));
      for (EndPointConnectionFactory endPointConnectionFactory : endPointConnections) {
        if (endPointConnectionFactory.getName().equals(endPointURL.getProtocol())) {
          EndPointURL url = new EndPointURL(properties.getProperty("url"));
          EndPointConnectionHostJMX hostJMXBean = hostMapping.computeIfAbsent(url.host, k -> new EndPointConnectionHostJMX(jmxParent, url.host));
          endPointConnectionList.add(new EndPointConnection(url, properties, endPointConnectionFactory, selectorLoadManager, hostJMXBean));
        }
      }
    }
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
