package io.mapsmessaging.network.protocol.impl.coap.packet;

import static io.mapsmessaging.network.protocol.impl.coap.packet.PacketFactory.IPATCH;

import io.mapsmessaging.network.io.Packet;

public class IPatch extends BasePacket {

  public IPatch(Packet packet) {
    super(IPATCH, packet);
  }

  @Override
  public String toString() {
    return super.toString();
  }
}