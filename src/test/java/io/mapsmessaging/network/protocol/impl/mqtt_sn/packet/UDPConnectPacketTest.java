package io.mapsmessaging.network.protocol.impl.mqtt_sn.packet;

import java.io.IOException;
import java.net.InetSocketAddress;

public class UDPConnectPacketTest extends ConnectPacketTest{

  PacketTransport createTransport(String host) throws Exception {
    return new UDPPacketTransport(
        new InetSocketAddress(host, 0),
        new InetSocketAddress(host, 1884));
  }

}
