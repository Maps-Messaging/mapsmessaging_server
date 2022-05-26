package io.mapsmessaging.network.protocol.impl.coap.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import java.net.SocketAddress;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class BasePacket implements ServerPacket {

  @Getter
  @Setter
  private byte[] token;

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
  @Setter Clazz clazz;

  @Getter
  @Setter TYPE type;

  @Getter
  @Setter
  int tokenLength;

  @Getter
  @Setter
  int code;

  @Getter
  @Setter
  int messageId;

  public BasePacket(Packet packet){
    byte val = packet.get();
    version =(val>>6 & 0b11);
    type = TYPE.valueOf((val>>4) & 0b11);
    tokenLength = (val) & 0b1111;

    val = packet.get();
    clazz = Clazz.valueOf((val >>5)& 0b111);
    code = val & 0b11111;

    messageId = (packet.get() & 0xff) << 8;
    messageId += (packet.get() & 0xff);
    token = new byte[tokenLength];
    packet.get(token);
  }


  @Override
  public int packFrame(Packet packet) {
    packet.put((byte)( (version & 0b11)<<6 | ((type.getValue() & 0b11) << 4 ) | (tokenLength & 0b1111)));
    packet.put((byte)(( (clazz.getValue() & 0b111) << 5) | (code & 0b11111)));
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
