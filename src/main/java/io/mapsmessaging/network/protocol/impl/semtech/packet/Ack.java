package io.mapsmessaging.network.protocol.impl.semtech.packet;

import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.VERSION;

import io.mapsmessaging.network.io.Packet;
import java.net.SocketAddress;
import lombok.Getter;

public abstract class Ack extends SemTechPacket {

  @Getter
  private final int token;
  private final int type;

  protected Ack(int token, int type, SocketAddress fromAddress) {
    super(fromAddress);
    this.token = token;
    this.type = type;
  }

  public int packFrame(Packet packet) {
    packet.putByte(VERSION);
    packet.putShort(token);
    packet.putByte(type);
    return 4;
  }
}
