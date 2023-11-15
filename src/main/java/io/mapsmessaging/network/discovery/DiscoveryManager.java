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

import io.mapsmessaging.BuildInfo;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.consul.ConsulManagerFactory;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.protocol.ProtocolFactory;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.rest.RestApiServerManager;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import io.mapsmessaging.utilities.service.Service;
import lombok.Getter;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

import static io.mapsmessaging.logging.ServerLogMessages.DISCOVERY_FAILED_TO_REGISTER;

public class DiscoveryManager implements Agent {
  private static final String ALL_HOSTS = "0.0.0.0";

  private final Logger logger;
  private final String serverName;
  private final List<AdapterManager> boundedNetworks;
  private final ConfigurationProperties properties;
  @Getter
  private final boolean enabled;

  public DiscoveryManager(String serverName) {
    this.serverName = serverName;
    logger = LoggerFactory.getLogger(DiscoveryManager.class);
    boundedNetworks = new ArrayList<>();
    properties = ConfigurationManager.getInstance().getProperties("DiscoveryManager");
    enabled = properties.getBooleanProperty("enabled", true);
  }

  public void registerListener(String type, ServiceListener listener){
    for(AdapterManager adapterManager:boundedNetworks){
      adapterManager.registerListener(type, listener);
    }
  }

  public void removeListener(String type, ServiceListener listener){
    for(AdapterManager adapterManager:boundedNetworks){
      adapterManager.removeListener(type, listener);
    }
  }

  @Override
  public String getName() {
    return "Discovery Manager";
  }

  @Override
  public String getDescription() {
    return "Manages the mDNS records";
  }

  public void start() {
    if (enabled) {
      boolean stampMeta = properties.getBooleanProperty("addTxtRecords", false);
      String hostnames = properties.getProperty("hostnames");
      try {
        if (hostnames != null) {
          String[] hostnameList = hostnames.split(",");
          for (String hostname : hostnameList) {
            InetAddress address = InetAddress.getByName(hostname.trim());
            boundedNetworks.add(bindInterface(hostname, address, stampMeta));
          }
        } else {
          InetAddress address = InetAddress.getLocalHost();
          boundedNetworks.add(bindInterface(address.getHostName(), address, stampMeta));
        }
      } catch (IOException e) {
        logger.log(ServerLogMessages.DISCOVERY_FAILED_TO_START, e);
      }
    }
  }

  public void stop() {
    Thread t = new Thread(this::deregisterAll);
    t.start();
  }

  private AdapterManager bindInterface(String hostname, InetAddress homeAddress, boolean stampMeta) throws IOException {
    return new AdapterManager(hostname, serverName, JmDNS.create(homeAddress, serverName), stampMeta);
  }

  public ServiceInfo[] register(RestApiServerManager restApiServerManager) {
    List<ServiceInfo> registeredServices = new ArrayList<>();
    for (AdapterManager manager : boundedNetworks) {
      Map<String, String> map = new LinkedHashMap<>();
      map.put("server name", MessageDaemon.getInstance().getId());
      map.put("schema support", "true");
      map.put("schema name", "$schema");
      map.put("version", BuildInfo.getBuildVersion());
      map.put("date", BuildInfo.getBuildDate());
      map.put("restApi", "true");
      String service = "_maps._tcp._local";
      if(restApiServerManager.isSecure()){
        map.put("protocol", "https");
      }
      else{
        map.put("protocol", "http");
      }

      ServiceInfo serviceInfo = ServiceInfo.create(service, serverName, restApiServerManager.getPort(), 0, 0, map);
      String host = restApiServerManager.getHost();
      if (host.equals(ALL_HOSTS) || host.equals(manager.getAdapter())) {
        try {
          manager.register(serviceInfo);
        } catch (IOException e) {
          logger.log(DISCOVERY_FAILED_TO_REGISTER, e);
          return new ServiceInfo[0];
        }
      }
      registeredServices.add(serviceInfo);
    }
    ConsulManagerFactory.getInstance().register(restApiServerManager);
    return registeredServices.toArray(new ServiceInfo[0]);
  }

  public void register(EndPointServer endPointServer) {
    if (!endPointServer.getConfig().getProperties().getBooleanProperty("discoverable", false)) {
      return;
    }
    EndPointURL url = endPointServer.getUrl();
    boolean isUDP = (url.getProtocol().equals("udp") || url.getProtocol().equals("hmac"));
    String transport = isUDP ? "udp" : "tcp";
    String protocolConfig = endPointServer.getConfig().getProtocols();
    List<String> protocolList = createProtocolList(protocolConfig, transport);
    String endPointHostName = endPointServer.getUrl().getHost();
    for(AdapterManager manager:boundedNetworks){
      if (endPointHostName.equals(ALL_HOSTS) || endPointHostName.equals(manager.getAdapter())) {
        manager.register(endPointServer, transport, protocolList);
      }
    }
    ConsulManagerFactory.getInstance().register(endPointServer);
  }

  private List<String> createProtocolList(String protocolConfig, String transport){
    String[] protocols = protocolConfig.split(",");
    List<String> protocolList = new ArrayList<>();
    for(String protocol:protocols){
      if(protocol.equalsIgnoreCase("all")){
        createProtocolList(protocol, transport, protocolList);
      }
      else{
        protocolList.add(protocol);
      }
    }
    return protocolList;
  }

  private void createProtocolList(String protocol, String transport, List<String> protocolList){
    ProtocolFactory protocolFactory = new ProtocolFactory(protocol);
    for (Iterator<Service> it = protocolFactory.getServices(); it.hasNext(); ) {
      ProtocolImplFactory impl = (ProtocolImplFactory) it.next();
      if (!impl.getName().equals("echo") &&
          impl.getTransportType().equals(transport)) {
        protocolList.add(impl.getName());
      }
    }
  }

  public synchronized List<ServiceInfo> register(String host, String type, String name, int port, String text) throws IOException {
    List<ServiceInfo> list = new ArrayList<>();
    for(AdapterManager manager:boundedNetworks) {
      if (host.equals(ALL_HOSTS) || host.equals(manager.getAdapter())) {
        ServiceInfo serviceInfo = ServiceInfo.create(type, name, port, text);
        manager.register(serviceInfo);
        list.add(serviceInfo);
      }
    }
    return list;
  }

  public synchronized void deregister(EndPointServer endPointServer) {
    for(AdapterManager manager:boundedNetworks){
      manager.deregister(endPointServer);
    }
  }

  public synchronized void deregister(ServiceInfo info) {
    for(AdapterManager manager:boundedNetworks){
      manager.deregister(info);
    }
  }

  public synchronized void deregisterAll() {
    for(AdapterManager manager:boundedNetworks){
      manager.deregisterAll();
    }
  }
}
