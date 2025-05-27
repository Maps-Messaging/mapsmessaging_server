package io.mapsmessaging.network.protocol.impl.proxy;

import lombok.Data;

import java.net.InetSocketAddress;

@Data
public class ProxyProtocolInfo {
  private final InetSocketAddress source;
  private final InetSocketAddress destination;
  private final String protocolVersion;
}