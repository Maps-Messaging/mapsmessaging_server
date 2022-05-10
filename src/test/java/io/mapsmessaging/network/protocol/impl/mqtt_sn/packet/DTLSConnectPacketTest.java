package io.mapsmessaging.network.protocol.impl.mqtt_sn.packet;

import java.net.InetSocketAddress;

public class DTLSConnectPacketTest extends ConnectPacketTest{

  @Override
  PacketTransport createTransport(InetSocketAddress client, InetSocketAddress server) throws Exception {
    return new DTLSPacketTransport(client, server);
  }
}
