package io.mapsmessaging.network.protocol.impl.mavlink;

import lombok.Data;
import lombok.ToString;

import java.net.InetSocketAddress;

@Data
@ToString
public class MavlinkDeviceKey {
  private final int localPort;
  private final InetSocketAddress remoteAddress;
  private final int remotePort;
  private final int systemId;
  private final int hash;

  public MavlinkDeviceKey(int localPort,
                          InetSocketAddress remoteAddress,
                          int systemId
                          ){
    this.localPort = localPort;
    this.remoteAddress = remoteAddress;
    this.remotePort = remoteAddress.getPort();
    this.systemId = systemId;
    this.hash = computeHash();
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MavlinkDeviceKey other)) return false;
    return localPort == other.localPort
        && remotePort == other.remotePort
        && systemId == other.systemId
        && remoteAddress.equals(other.remoteAddress);
  }

  private int computeHash() {
    int h = 17;
    h = 31 * h + localPort;
    h = 31 * h + remoteAddress.hashCode();
    h = 31 * h + systemId;
    return h;
  }
}
