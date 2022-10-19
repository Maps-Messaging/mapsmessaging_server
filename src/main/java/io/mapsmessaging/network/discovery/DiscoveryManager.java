package io.mapsmessaging.network.discovery;

import io.mapsmessaging.BuildInfo;
import io.mapsmessaging.MessageDaemon;
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
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

public class DiscoveryManager implements Agent {

  private final Logger logger;
  private final String serverName;
  private final List<AdapterManager> boundedNetworks;

  public DiscoveryManager(String serverName) {
    this.serverName = serverName;
    logger = LoggerFactory.getLogger(DiscoveryManager.class);
    boundedNetworks = new ArrayList<>();
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
    ConfigurationProperties properties = ConfigurationManager.getInstance().getProperties("DiscoveryManager");
    if (properties.getBooleanProperty("enabled", false)) {
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

  public ServiceInfo register(RestApiServerManager restApiServerManager) {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("server name", MessageDaemon.getInstance().getId());
    map.put("schema support", "true");
    map.put("schema name", "$schema");
    map.put("version", BuildInfo.getInstance().getBuildVersion());
    map.put("date", BuildInfo.getInstance().getBuildDate());
    map.put("restApi", "true");
    String service = "_maps._tcp._local";
    ServiceInfo serviceInfo = ServiceInfo.create(service, serverName, restApiServerManager.getPort(), 0, 0, map);
    for (AdapterManager manager : boundedNetworks) {
      String host = restApiServerManager.getHost();
      if (host.equals("0.0.0.0") || host.equals(manager.getAdapter())) {
        try {
          manager.register(serviceInfo);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return serviceInfo;
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
      if(endPointHostName.equals("0.0.0.0") || endPointHostName.equals(manager.getAdapter())){
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
      if (host.equals("0.0.0.0") || host.equals(manager.getAdapter())) {
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
