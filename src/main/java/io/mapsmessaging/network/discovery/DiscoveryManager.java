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

package io.mapsmessaging.network.discovery;

import io.mapsmessaging.BuildInfo;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.config.DiscoveryManagerConfig;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.monitor.NetworkEvent;
import io.mapsmessaging.network.monitor.NetworkInterfaceMonitor;
import io.mapsmessaging.network.monitor.NetworkStateChange;
import io.mapsmessaging.network.protocol.ProtocolFactory;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.rest.RestApiServerManager;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.service.Service;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static io.mapsmessaging.logging.ServerLogMessages.DISCOVERY_FAILED_TO_REGISTER;

public class DiscoveryManager implements Agent, Consumer<NetworkStateChange> {
  private static final String ALL_HOSTS = "::";
  private static final String ALL_HOSTS_V4 = "0.0.0.0";

  private final Logger logger;
  private final String serverName;
  @Getter
  private final List<AdapterManager> boundedNetworks;
  private final DiscoveryManagerConfig properties;
  private final boolean stampMeta;
  private final String domainName;
  @Getter
  private final boolean enabled;

  public DiscoveryManager(String serverName) {
    this.serverName = serverName;
    logger = LoggerFactory.getLogger(DiscoveryManager.class);
    boundedNetworks = new ArrayList<>();
    properties = DiscoveryManagerConfig.getInstance();
    enabled = properties.isEnabled();
    stampMeta = properties.isAddTxtRecords();
    domainName = properties.getDomainName();

    java.util.logging.Logger utilLogger = java.util.logging.Logger.getLogger("javax.jmdns");
    utilLogger.setLevel(Level.OFF); // Set to OFF to disable logging

    // If JmDNS uses specific child loggers, they must also be adjusted
    utilLogger.setUseParentHandlers(false); // Stop log messages from propagating to the parent handlers
    for (java.util.logging.Handler handler : utilLogger.getHandlers()) {
      handler.setLevel(Level.OFF); // Optionally disable each handler
    }
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
      try {
        NetworkAddressHelper networkAddressHelper = new NetworkAddressHelper();
        bindAddresses(networkAddressHelper.getAddresses(properties.getHostnames()));
      } catch (IOException e) {
        logger.log(ServerLogMessages.DISCOVERY_FAILED_TO_START, e);
      }
      NetworkInterfaceMonitor.getInstance().addListener(this);
    }
  }

  private void bindAddresses( List<InetAddress> addresses) throws IOException {
    for (InetAddress address : addresses) {
      boundedNetworks.add(bindInterface(address, stampMeta));
    }
  }

  public void stop() {
    Thread t = new Thread(this::deregisterAll);
    t.start();
  }

  @Override
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName(getName());
    status.setComment("");
    if(enabled){
      if(boundedNetworks.isEmpty()){
        status.setStatus(Status.WARN);
        status.setComment("No bound networks");
      }
      else{
        status.setStatus(Status.OK);
      }
    }
    else{
      status.setStatus(Status.DISABLED);
    }
    return status;
  }


  private AdapterManager bindInterface(InetAddress homeAddress, boolean stampMeta) throws IOException {
    return new AdapterManager(homeAddress.getHostAddress(), serverName, JmDNS.create(homeAddress, serverName), stampMeta, domainName);
  }

  public ServiceInfo[] register(RestApiServerManager restApiServerManager) {
    List<ServiceInfo> registeredServices = new ArrayList<>();
    for (AdapterManager manager : boundedNetworks) {
      Map<String, String> map = new LinkedHashMap<>();
      map.put("server name", MessageDaemon.getInstance().getId());
      map.put("schema support", "true");
      map.put("schema prefix", DestinationMode.SCHEMA.getNamespace());
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
      if (host.equals(ALL_HOSTS) ||
          host.equals(ALL_HOSTS_V4) ||
          host.equals(manager.getAdapter())) {
        try {
          manager.register(serviceInfo);
        } catch (IOException e) {
          logger.log(DISCOVERY_FAILED_TO_REGISTER, e);
          return new ServiceInfo[0];
        }
      }
      registeredServices.add(serviceInfo);
    }
    return registeredServices.toArray(new ServiceInfo[0]);
  }

  public void register(EndPointServer endPointServer) {
    if (!endPointServer.getConfig().getEndPointConfig().isDiscoverable()) {
      return;
    }
    EndPointURL url = endPointServer.getUrl();
    boolean isUDP = (url.getProtocol().equals("udp") || url.getProtocol().equals("hmac"));
    String transport = isUDP ? "udp" : "tcp";
    String protocolConfig = endPointServer.getConfig().getProtocols();
    List<String> protocolList = createProtocolList(protocolConfig, transport);
    String endPointHostName = endPointServer.getUrl().getHost();
    for(AdapterManager manager:boundedNetworks){
      if (endPointHostName.equals(ALL_HOSTS) ||
          endPointHostName.equals(ALL_HOSTS_V4) ||
          endPointHostName.equals(manager.getAdapter())) {
        manager.register(endPointServer, transport, protocolList);
      }
    }
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

  private boolean addInterface(InetAddress inetAddress) {
    String hostnames = properties.getHostnames();
    if (hostnames != null) {
      String[] hostnameList = hostnames.split(",");
      for (String hostname : hostnameList) {
        if (NetworkInterfaceMonitor.getInstance().ipAddressMatches(hostname.trim(), inetAddress)) {
          return true;
        }
      }
    }
    return false;
  }


  @Override
  public void accept(NetworkStateChange networkStateChange) {
    if (networkStateChange.getEvent() == NetworkEvent.IP_CHANGED || networkStateChange.getEvent() == NetworkEvent.ADDED) {
      handleAdded(networkStateChange);
    } else if (networkStateChange.getEvent() == NetworkEvent.DOWN || networkStateChange.getEvent() == NetworkEvent.REMOVED) {
      handleRemoved(networkStateChange);
    }
  }

  private void handleAdded(NetworkStateChange networkStateChange){
    for (InetAddress address : networkStateChange.getNetworkInterface().getIpAddresses()) {
      if (addInterface(address)) {
        try {
          boolean found = boundedNetworks.stream().anyMatch(manager -> manager.getAdapter().equals(address.getHostAddress()));
          if (!found) boundedNetworks.add(bindInterface(address, stampMeta));
        } catch (IOException e) {
          logger.log(ServerLogMessages.DISCOVERY_FAILED_TO_START, e);
        }
      }
    }
  }

  private void handleRemoved(NetworkStateChange networkStateChange){
    for (InetAddress address : networkStateChange.getNetworkInterface().getIpAddresses()) {
      String name = address.getHostAddress();
      List<AdapterManager> toRemove = boundedNetworks.stream().filter(manager -> manager.getAdapter().equals(name)).collect(Collectors.toList());
      for (AdapterManager manager : toRemove) {
        boundedNetworks.remove(manager);
        try {
          manager.close();
        } catch (IOException e) {
          // Log It
        }
      }
    }
  }

  @Override
  public @NotNull Consumer<NetworkStateChange> andThen(Consumer<? super NetworkStateChange> after) {
    Objects.requireNonNull(after);
    return Consumer.super.andThen(after);
  }
}
