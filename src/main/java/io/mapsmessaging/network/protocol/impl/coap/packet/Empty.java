package io.mapsmessaging.network.protocol.impl.coap.packet;

import static io.mapsmessaging.network.protocol.impl.coap.packet.PacketFactory.EMPTY;

import io.mapsmessaging.network.io.Packet;

public class Empty extends BasePacket {

  public Empty(Packet packet) {
    super(EMPTY, packet);
  }
}