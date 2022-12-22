package io.mapsmessaging.network.io.impl.hmac;

import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.NetworkConfig;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.network.io.impl.udp.UDPEndPoint;
import io.mapsmessaging.network.io.impl.udp.UDPEndPointServer;
import io.mapsmessaging.network.io.security.NodeSecurity;
import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.PacketIntegrityFactory;
import io.mapsmessaging.network.protocol.ProtocolFactory;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HmacUDPEndPointServer extends UDPEndPointServer {

  private final Map<String, NodeSecurity> securityMap;

  public HmacUDPEndPointServer(InetSocketAddress inetSocketAddress, ProtocolFactory protocolFactory, EndPointURL url,
      SelectorLoadManager selectorLoadManager, EndPointManagerJMX managerMBean,
      NetworkConfig config) throws SocketException {
    super(inetSocketAddress, protocolFactory, url, selectorLoadManager, managerMBean, config);
    securityMap = new LinkedHashMap<>();
    ConfigurationProperties props = getConfig().getProperties();
    Object t = props.get("nodeConfiguration");
    loadNodeConfig((List<ConfigurationProperties>) t);
  }

  private void loadNodeConfig(List<ConfigurationProperties> nodes) {
    for (ConfigurationProperties node : nodes) {
      String host = node.getProperty("host");
      int port = node.getIntProperty("port", 0);
      PacketIntegrity packetIntegrity = PacketIntegrityFactory.getInstance().createPacketIntegrity(node);
      securityMap.put(host + ":" + port, new NodeSecurity(host, port, packetIntegrity));
    }
  }

  @Override
  protected UDPEndPoint createEndPoint(InetSocketAddress bonded) throws IOException {
    return new HmacUDPEndPoint(
        bonded,
        selectorLoadManager.allocate(),
        1,
        this,
        authenticationConfig,
        managerMBean,
        securityMap
    );
  }
}