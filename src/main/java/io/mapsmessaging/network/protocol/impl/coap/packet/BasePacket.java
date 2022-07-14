package io.mapsmessaging.network.protocol.impl.coap.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Option;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.OptionSet;
import java.io.IOException;
import java.net.SocketAddress;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
public class BasePacket implements ServerPacket {

  @Getter
  private final int id;

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
  @Setter
  Clazz clazz;

  @Getter
  @Setter
  TYPE type;

  @Getter
  @Setter
  int tokenLength;

  @Getter
  @Setter
  int code;

  @Getter
  @Setter
  int messageId;

  @Getter
  OptionSet options;

  @Getter
  @Setter
  byte[] payload;

  public BasePacket(int id, Packet packet) {
    this.id = id;
    byte val = packet.get();
    version = (val >> 6 & 0b11);
    type = TYPE.valueOf((val >> 4) & 0b11);
    tokenLength = (val) & 0b1111;

    val = packet.get();
    clazz = Clazz.valueOf((val >> 5) & 0b111);
    code = val & 0b11111;

    messageId = (packet.get() & 0xff) << 8;
    messageId += (packet.get() & 0xff);
    token = new byte[tokenLength];
    packet.get(token);

    options = new OptionSet();
  }


  @Override
  public int packFrame(Packet packet) {
    packet.put((byte) ((version & 0b11) << 6 | ((type.getValue() & 0b11) << 4) | (tokenLength & 0b1111)));
    packet.put((byte) (((clazz.getValue() & 0b111) << 5) | (code & 0b11111)));
    packet.put((byte) (messageId >> 8 & 0xff));
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

  protected void readOptions(@NotNull Packet packet) throws IOException{
    int optionNumber = 0;
    while(packet.hasData()){
      int val = packet.get() & 0xff;
      if(val == 0xFF) return; // Found payload flag
      optionNumber += readVariableInt(packet, val >> 4);
      int optionLength = readVariableInt(packet, val & 0xf);
      byte[] data = new byte[optionLength];
      packet.get(data);
      Option option = options.getOption(optionNumber);
      option.update(data);
    }
  }


  private static int readVariableInt(Packet packet, int val) throws IOException {
    if (val <= 12) {
      return val;
    } else if (val == 13) {
      return (packet.get()& 0xff) + 13;
    } else if (val == 14) {
      val = (packet.get() & 0xff) << 8;
      val = val | (packet.get() & 0xff);
      return val + 269;
    } else {
      throw new IOException("Invalid variable int header found");
    }
  }

  public void readPayload(Packet packet) {
    // The 0xff has already been stripped
    int size = packet.available();
    payload = new byte[size];
    packet.get(payload);
  }
}
