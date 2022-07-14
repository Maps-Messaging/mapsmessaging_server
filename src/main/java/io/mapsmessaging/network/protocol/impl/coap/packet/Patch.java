package io.mapsmessaging.network.protocol.impl.coap.packet;

import static io.mapsmessaging.network.protocol.impl.coap.packet.PacketFactory.PATCH;

import io.mapsmessaging.network.io.Packet;

public class Patch extends BasePacket {

  public Patch(Packet packet) {
    super(PATCH, packet);
  }

  @Override
  public String toString() {
    return super.toString();
  }
}