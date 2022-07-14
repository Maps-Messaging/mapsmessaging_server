package io.mapsmessaging.network.protocol.impl.coap.packet;

import io.mapsmessaging.network.io.Packet;
import java.io.IOException;

public class PacketFactory {

  public static final int EMPTY = 0;
  public static final int GET = 1;
  public static final int POST = 2;
  public static final int PUT = 3;
  public static final int DELETE = 4;
  public static final int FETCH = 5;
  public static final int PATCH = 6;
  public static final int IPATCH = 7;


  public BasePacket parseFrame(Packet packet) throws IOException {
    byte val = packet.get(packet.position() + 1);
    int code = val & 0b11111;
    BasePacket basePacket;
    switch (code) {
      case EMPTY:
        basePacket = new Empty( packet);
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
        basePacket = new BasePacket(0, packet);
    }
    basePacket.readOptions(packet);
    basePacket.readPayload(packet);
    basePacket.setFromAddress(packet.getFromAddress());
    return basePacket;
  }
}
