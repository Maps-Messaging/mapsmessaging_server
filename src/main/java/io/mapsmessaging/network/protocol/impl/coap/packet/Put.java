package io.mapsmessaging.network.protocol.impl.coap.packet;

import static io.mapsmessaging.network.protocol.impl.coap.packet.PacketFactory.PUT;

import io.mapsmessaging.network.io.Packet;

public class Put extends BasePacket {

  public Put(Packet packet) {
    super(PUT, packet);
  }

  @Override
  public String toString() {
    return super.toString();
  }
}