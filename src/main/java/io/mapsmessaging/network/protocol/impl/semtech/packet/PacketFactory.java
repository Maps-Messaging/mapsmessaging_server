package io.mapsmessaging.network.protocol.impl.semtech.packet;

import io.mapsmessaging.network.io.Packet;
import java.io.IOException;

public class PacketFactory {

  public static final int VERSION = 2;

  public static final int PUSH_DATA = 0x0;
  public static final int PUSH_ACK = 0x1;
  public static final int PULL_DATA = 0x2;
  public static final int PULL_RESPONSE = 0x3;
  public static final int PULL_ACK = 0x4;
  public static final int TX_ACK = 0x5;
  public static final int MAX_EVENTS = 0x6;

  public SemTechPacket parse(Packet packet) throws IOException {
    int version = packet.get();
    if (version != 2) {
      throw new IOException("Unsupported version detected");
    }
    int token = packet.getShort();
    int identifier = (packet.get() & 0xff);
    switch (identifier) {
      case PUSH_DATA:
        return new PushData(token, packet);

      case PULL_DATA:
        return new PullData(token, packet);

      case PULL_ACK:
        return new PullAck(token, packet.getFromAddress());

      case TX_ACK:
        return new TxAcknowledge(token, packet);

      default:
        return null;
    }
  }

}
