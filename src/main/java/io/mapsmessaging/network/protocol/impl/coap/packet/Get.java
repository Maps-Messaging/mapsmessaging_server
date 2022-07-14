package io.mapsmessaging.network.protocol.impl.coap.packet;

import static io.mapsmessaging.network.protocol.impl.coap.packet.PacketFactory.GET;

import io.mapsmessaging.network.io.Packet;

public class Get extends BasePacket {

  public Get(Packet packet) {
    super(GET, packet);
  }

  @Override
  public String toString() {
    return super.toString();
  }
}
