package io.mapsmessaging.network.protocol.impl.proxy;

import io.mapsmessaging.network.io.Packet;

import java.net.UnknownHostException;

public abstract class ProxyProtocol {
  public abstract boolean matches(Packet packet);

  public abstract ProxyProtocolInfo parse(Packet packet) throws UnknownHostException;
}
