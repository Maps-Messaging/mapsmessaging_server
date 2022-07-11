package io.mapsmessaging.network.discovery;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.protocol.ProtocolFactory;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import io.mapsmessaging.utilities.service.Service;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

public class DiscoveryManager {

  private final Logger logger;
  private final String serverName;
  private final List<AdapterManager> boundedNetworks;

  public DiscoveryManager(String serverName) {
    this.serverName = serverName;
    logger = LoggerFactory.getLogger(DiscoveryManager.class);
    boundedNetworks = new ArrayList<>();

    ConfigurationProperties properties = ConfigurationManager.getInstance().getProperties("DiscoveryManager");
    if (properties.getBooleanProperty("enabled", false)) {
      boolean stampMeta = properties.getBooleanProperty("addTxtRecords", false);
      String hostnames = properties.getProperty("hostnames");
      try {
        if(hostnames != null){
          String[] hostnameList = hostnames.split(",");
          for(String hostname: hostnameList){
            InetAddress address = InetAddress.getByName(hostname.trim());
            boundedNetworks.add(bindInterface(hostname, address, stampMeta));
          }
        }
        else{
          InetAddress address = InetAddress.getLocalHost();
          boundedNetworks.add(bindInterface(address.getHostName(), address, stampMeta));
        }
      } catch (IOException e) {
        logger.log(ServerLogMessages.DISCOVERY_FAILED_TO_START, e);
      }
    }
  }

  private AdapterManager bindInterface(String hostname, InetAddress homeAddress, boolean stampMeta) throws IOException {
    return new AdapterManager(hostname, serverName, JmDNS.create(homeAddress, serverName), stampMeta);
  }


  public void register(EndPointServer endPointServer) {
    if(!endPointServer.getConfig().getProperties().getBooleanProperty("discoverable", false)){
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
        ProtocolFactory protocolFactory = new ProtocolFactory(protocol);
        for (Iterator<Service> it = protocolFactory.getServices(); it.hasNext(); ) {
          ProtocolImplFactory impl = (ProtocolImplFactory)it.next();
          if(impl.getTransportType().equals(transport)) {
            protocolList.add(impl.getName());
          }
        }
      }
      else{
        protocolList.add(protocol);
      }
    }
    return protocolList;
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
