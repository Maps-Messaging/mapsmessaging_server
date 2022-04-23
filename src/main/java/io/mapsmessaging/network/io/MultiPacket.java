package io.mapsmessaging.network.io;

import java.nio.ByteBuffer;

public class MultiPacket extends Packet {

  public MultiPacket(int size, boolean direct) {
    super(size, direct);
  }

  public MultiPacket(ByteBuffer buffer) {
    super(buffer);
  }

  protected MultiPacket(Packet packet) {
    super(packet);
  }
}
