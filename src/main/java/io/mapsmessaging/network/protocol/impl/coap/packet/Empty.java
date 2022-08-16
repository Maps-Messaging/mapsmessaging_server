package io.mapsmessaging.network.protocol.impl.coap.packet;

import static io.mapsmessaging.network.protocol.impl.coap.packet.PacketFactory.EMPTY;

import io.mapsmessaging.network.io.Packet;

public class Empty extends BasePacket {

  public Empty(Packet packet) {
    super(EMPTY, packet);
  }

  public Empty(int messageId){
    super(EMPTY, TYPE.RST, Code.EMPTY, 1, messageId, new byte[0]);
  }

  public Empty(TYPE type, Code code, int version, int messageId, byte[] token){
    super(EMPTY, type, code, version, messageId, token);
  }
}