package io.mapsmessaging.network.protocol.impl.semtech.packet;

import io.mapsmessaging.network.io.ServerPacket;
import java.net.SocketAddress;
import lombok.Getter;

public abstract class SemTechPacket implements ServerPacket {


  @Getter
  SocketAddress fromAddress;

  public SemTechPacket(SocketAddress fromAddress) {
    super();
    this.fromAddress = fromAddress;
  }

  @Override
  public void complete() {}

  abstract public int getIdentifier();

}
