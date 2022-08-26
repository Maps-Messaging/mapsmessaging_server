package io.mapsmessaging.network.protocol.impl.coap.blockwise;

import lombok.Getter;
import lombok.Setter;

public class BlockReceiveState {

  @Getter
  @Setter
  private long lastAccess;

  @Getter
  private final ReceivePacket receivePacket;

  public BlockReceiveState(ReceivePacket receivePacket) {
    this.receivePacket = receivePacket;
    lastAccess = System.currentTimeMillis();
  }
}
