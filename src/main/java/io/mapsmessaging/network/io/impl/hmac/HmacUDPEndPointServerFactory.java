package io.mapsmessaging.network.io.impl.hmac;

import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.NetworkConfig;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.EndPointServerFactory;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.network.protocol.ProtocolFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class HmacUDPEndPointServerFactory implements EndPointServerFactory {

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
    return new HmacUDPEndPointServer(inetSocketAddress, protocolFactory, url, selector, managerMBean, config);
  }

  @Override
  public String getName() {
    return "hmac";
  }

  @Override
  public String getDescription() {
    return "UDP HMAC End Point Server Factory";
  }


  @Override
  public boolean active() {
    return true;
  }

}
