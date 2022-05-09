package io.mapsmessaging.network.io.impl.dtls;

import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.NetworkConfig;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.network.io.impl.udp.UDPEndPointServerFactory;
import io.mapsmessaging.network.protocol.ProtocolFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class DTLSEndPointServerFactory extends UDPEndPointServerFactory {

  @Override
  public EndPointServer instance(
      EndPointURL url,
      SelectorLoadManager selector,
      AcceptHandler acceptHandler,
      NetworkConfig config,
      EndPointManagerJMX managerMBean)
      throws IOException {

    InetAddress bindAddress = InetAddress.getByName(url.getHost());
    InetSocketAddress inetSocketAddress = new InetSocketAddress(bindAddress, url.getPort());
    ProtocolFactory protocolFactory = new ProtocolFactory(config.getProtocols());
    return new DTLSEndPointServer(inetSocketAddress, protocolFactory, url, selector,acceptHandler, managerMBean, config);
  }

  @Override
  public String getName() {
    return "dtls";
  }

  @Override
  public String getDescription() {
    return "Encrypted Datagram End Point Server Factory";
  }


  @Override
  public boolean active() {
    return true;
  }

}
