package io.mapsmessaging.network.protocol.impl.coap.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;

public class PacketFactory {

  private static final int EMPTY = 0;
  private static final int GET = 1;
  private static final int POST = 2;
  private static final int PUT = 3;
  private static final int DELETE = 4;
  private static final int FETCH = 5;
  private static final int PATCH = 6;
  private static final int IPATCH = 7;


  public BasePacket parseFrame(Packet packet) throws EndOfBufferException{
    byte val = packet.get(packet.position()+1);
    int code = val & 0b11111;
    BasePacket basePacket;
    switch (code) {
      case EMPTY:
        basePacket = new Empty(packet);
        break;
      case GET:
        basePacket = new Get(packet);
        break;
      case DELETE:
        basePacket = new Delete(packet);
        break;
      case POST:
        basePacket = new Post(packet);
        break;
      case PUT:
        basePacket = new Put(packet);
        break;
      case FETCH:
        basePacket = new Fetch(packet);
        break;
      case PATCH:
        basePacket = new Patch(packet);
        break;
      case IPATCH:
        basePacket = new IPatch(packet);
        break;

      default:
        basePacket = new BasePacket(packet);
    }
    return basePacket;
  }
}
