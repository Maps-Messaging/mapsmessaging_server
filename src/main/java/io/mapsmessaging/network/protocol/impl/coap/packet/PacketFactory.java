package io.mapsmessaging.network.protocol.impl.coap.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;

public class PacketFactory {

  private static final int EMPTY = 0;
  private static final int GET = 1;
  private static final int POST = 2;
  private static final int PUT = 3;
  private static final int DELETE = 4;
  private static final int FETCH = 5;
  private static final int PATCH = 6;
  private static final int iPATCH = 7;


  public BasePacket parseFrame(Packet packet) throws EndOfBufferException{
    byte val = packet.get(packet.position()+1);
    int code = val & 0b11111;
    BasePacket basePacket = null;
    switch (code) {
      case EMPTY: {
        basePacket = new Empty(packet);
        break;
      }

      case GET: {
        basePacket = new Get(packet);
        break;
      }

      default:
        basePacket = new BasePacket(packet);
    }
    return basePacket;
  }
}
