package io.mapsmessaging.network.protocol.impl.coap.packet;

import static io.mapsmessaging.network.protocol.impl.coap.packet.PacketFactory.DELETE;

import io.mapsmessaging.network.io.Packet;

public class Delete extends BasePacket {

  public Delete(Packet packet) {
    super(DELETE, packet);
  }

  @Override
  public String toString() {
    return super.toString();
  }
}