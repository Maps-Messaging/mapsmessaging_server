package io.mapsmessaging.network.discovery;

import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

public class DiscoveryManager {

  private final String serverName;
  private final boolean enabled;
  private final JmDNS mDNSAgent;
  private final List<ServiceInfo> services;
  private final Map<EndPointServer, List<ServiceInfo>> endPointList;

  public DiscoveryManager(String serverName) {
    this.serverName = serverName;
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
        e.printStackTrace();
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
    if (!enabled)
      return;
    EndPointURL url = endPointServer.getUrl();
    boolean isUDP = (url.getProtocol().equals("udp") || url.getProtocol().equals("hmac"));
    String protocolList = endPointServer.getConfig().getProtocols();
    String[] protocols = protocolList.split(",");
    String interfaceName = endPointServer.getConfig().getProperties().getProperty("name", "");
    List<ServiceInfo> serviceInfo = new ArrayList<>();
    endPointList.put(endPointServer, serviceInfo);
    Runnable r = () -> {
      for (String protocol : protocols) {
        try {
          String service = "_" + protocol + "._tcp.local.";
          if (isUDP)
            service = "_" + protocol + "._udp.local.";
          serviceInfo.add(register(service, serverName + " " + interfaceName, url.getPort(), "/"));
        } catch (IOException e) {
          e.printStackTrace();
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
  }

  public synchronized void deregisterAll() {
    if (!enabled)
      return;
    mDNSAgent.unregisterAllServices();
  }
}
