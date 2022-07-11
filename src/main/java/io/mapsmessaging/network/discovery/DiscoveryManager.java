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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

public class DiscoveryManager {

  private final Logger logger;
  private final String serverName;
  private final boolean enabled;
  private final JmDNS mDNSAgent;
  private final List<ServiceInfo> services;
  private final Map<EndPointServer, List<ServiceInfo>> endPointList;

  public DiscoveryManager(String serverName) {
    this.serverName = serverName;
    logger = LoggerFactory.getLogger(DiscoveryManager.class);
    ConfigurationProperties properties = ConfigurationManager.getInstance().getProperties("DiscoveryManager");
    if (properties.getBooleanProperty("enabled", false)) {
      enabled = true;
      JmDNS agent = null;
      try {
        InetAddress homeAddress = InetAddress.getLocalHost();
        String hostname = properties.getProperty("hostname");
        if (hostname != null) {
          homeAddress = InetAddress.getByName(hostname);
        }
        agent = JmDNS.create(homeAddress, serverName);
      } catch (IOException e) {
        logger.log(ServerLogMessages.DISCOVERY_FAILED_TO_START, e);
      }
      mDNSAgent = agent;
    } else {
      enabled = false;
      mDNSAgent = null;
    }
    services = new CopyOnWriteArrayList<>();
    endPointList = new LinkedHashMap<>();
  }

  public void register(EndPointServer endPointServer) {
    if (!enabled || !endPointServer.getConfig().getProperties().getBooleanProperty("discoverable", false))
      return;

    EndPointURL url = endPointServer.getUrl();
    boolean isUDP = (url.getProtocol().equals("udp") || url.getProtocol().equals("hmac"));
    String transport = isUDP ? "udp" : "tcp";

    String protocolConfig = endPointServer.getConfig().getProtocols();
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
    String interfaceName = endPointServer.getConfig().getProperties().getProperty("name", "");
    List<ServiceInfo> serviceInfo = new ArrayList<>();
    endPointList.put(endPointServer, serviceInfo);
    Runnable r = () -> {
      for (String protocol : protocolList) {
        try {
          String service = "_" + protocol + "._"+transport+".local.";
          serviceInfo.add(register(service, serverName + " " + interfaceName, url.getPort(), "/"));
        } catch (IOException e) {
          logger.log(ServerLogMessages.DISCOVERY_FAILED_TO_REGISTER, e);
        }
      }
    };
    Thread t = new Thread(r);
    t.start();
  }

  public synchronized ServiceInfo register(String type, String name, int port, String text) throws IOException {
    if (!enabled)
      return null;
    ServiceInfo serviceInfo = ServiceInfo.create(type, name, port, text);
    mDNSAgent.registerService(serviceInfo);
    services.add(serviceInfo);
    return serviceInfo;
  }

  public synchronized void deregister(EndPointServer endPointServer) {
    if (!enabled)
      return;
    List<ServiceInfo> list = endPointList.remove(endPointServer);
    if (list != null) {
      for (ServiceInfo info : list) {
        deregister(info);
      }
    }
  }


  public synchronized void deregister(ServiceInfo info) {
    if (!enabled)
      return;
    mDNSAgent.unregisterService(info);
    services.remove(info);
  }

  public synchronized void deregisterAll() {
    if (!enabled)
      return;
    mDNSAgent.unregisterAllServices();
    services.clear();
  }
}
