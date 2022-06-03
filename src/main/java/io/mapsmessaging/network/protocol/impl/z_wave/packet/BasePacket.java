package io.mapsmessaging.network.protocol.impl.z_wave.packet;

import io.mapsmessaging.network.io.ServerPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public abstract class BasePacket implements ServerPacket {

  @Override
  public void complete() {

  }

  @Override
  public SocketAddress getFromAddress() {
    return InetSocketAddress.createUnresolved("127.0.0.1", 0);
  }

}
