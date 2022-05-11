package io.mapsmessaging.network.protocol.impl.mqtt_sn.packet;

import java.net.InetSocketAddress;

public class DTLSConnectPacketTest extends ConnectPacketTest{

  @Override
  PacketTransport createTransport(String host) throws Exception {
    return new DTLSPacketTransport(
        new InetSocketAddress(host, 0),
        new InetSocketAddress(host, 1886));
  }
}
