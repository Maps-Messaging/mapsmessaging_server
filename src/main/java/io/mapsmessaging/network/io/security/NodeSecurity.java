package io.mapsmessaging.network.io.security;

import lombok.Getter;

public class NodeSecurity {

  @Getter
  private final String host;
  @Getter
  private final int port;
  @Getter
  private final PacketIntegrity packetIntegrity;

  public NodeSecurity(String host, int port, PacketIntegrity packetIntegrity) {
    this.host = host;
    this.port = port;
    this.packetIntegrity = packetIntegrity;
  }
}
