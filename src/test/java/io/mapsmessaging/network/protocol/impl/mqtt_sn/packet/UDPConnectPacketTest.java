package io.mapsmessaging.network.protocol.impl.mqtt_sn.packet;

import java.io.IOException;
import java.net.InetSocketAddress;

public class UDPConnectPacketTest extends ConnectPacketTest{

  @Override
  PacketTransport createTransport(InetSocketAddress client, InetSocketAddress server) throws IOException {
    return new UDPPacketTransport(client, server);
  }
}
