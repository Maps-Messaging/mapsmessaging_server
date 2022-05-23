package io.mapsmessaging.network.protocol.impl.coap.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import java.net.SocketAddress;
import lombok.Getter;
import lombok.Setter;

public class BasePacket implements ServerPacket {

  @Getter
  @Setter
  private SocketAddress fromAddress;

  @Getter
  @Setter
  private Runnable callback;

  @Getter
  @Setter
  private int version;

  @Getter
  @Setter int type;

  @Getter
  @Setter
  int tokenLength;

  @Getter
  @Setter
  int code;

  @Getter
  @Setter
  int messageId;

  @Override
  public int packFrame(Packet packet) {
    packet.put((byte)( (version & 0b11) | ((type & 0b11) << 2 ) | (tokenLength & 0b1111) << 4));
    packet.put((byte)(code & 0xff));
    packet.put((byte) (messageId>>8 & 0xff));
    packet.put((byte) (messageId & 0xff));
    return 4;
  }

  @Override
  public void complete() {
    Runnable tmp;
    synchronized (this) {
      tmp = callback;
      callback = null;
    }
    if (tmp != null) {
      tmp.run();
    }
  }

}
