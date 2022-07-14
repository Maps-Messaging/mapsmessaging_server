package io.mapsmessaging.network.protocol.impl.coap.packet;

import static io.mapsmessaging.network.protocol.impl.coap.packet.PacketFactory.FETCH;

import io.mapsmessaging.network.io.Packet;

public class Fetch extends BasePacket {

  public Fetch(Packet packet) {
    super(FETCH, packet);
  }

  @Override
  public String toString() {
    return super.toString();
  }
}