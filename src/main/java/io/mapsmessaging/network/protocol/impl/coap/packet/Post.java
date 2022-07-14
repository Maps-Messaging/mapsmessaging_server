package io.mapsmessaging.network.protocol.impl.coap.packet;

import static io.mapsmessaging.network.protocol.impl.coap.packet.PacketFactory.POST;

import io.mapsmessaging.network.io.Packet;

public class Post extends BasePacket {

  public Post(Packet packet) {
    super(POST, packet);
  }

  @Override
  public String toString() {
    return super.toString();
  }
}
