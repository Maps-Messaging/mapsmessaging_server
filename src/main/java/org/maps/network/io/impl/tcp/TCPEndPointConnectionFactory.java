package org.maps.network.io.impl.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.List;
import org.maps.network.EndPointURL;
import org.maps.network.admin.EndPointConnectionJMX;
import org.maps.network.io.EndPoint;
import org.maps.network.io.EndPointConnectionFactory;
import org.maps.network.io.EndPointServerStatus;
import org.maps.network.io.impl.SelectorLoadManager;

public class TCPEndPointConnectionFactory extends EndPointConnectionFactory {

  @Override
  public EndPoint connect(EndPointURL url, SelectorLoadManager selector, EndPointServerStatus endPointServerStatus, List<String> jmxPath) throws IOException {
    SocketChannel channel = SocketChannel.open();
    InetSocketAddress address = new InetSocketAddress(url.getHost(), url.getPort());
    channel.connect(address);
    return new TCPEndPoint(generateID(), channel.socket(), selector.allocate(), endPointServerStatus, jmxPath);
  }

  @Override
  public String getName() {
    return Constants.name;
  }

  @Override
  public String getDescription() {
    return "tcp connection end point factory";
  }

}
