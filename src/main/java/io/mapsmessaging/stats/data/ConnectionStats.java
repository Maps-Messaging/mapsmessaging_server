package io.mapsmessaging.stats.data;

import lombok.Data;

@Data
public class ConnectionStats {
  private int currentConnections;
  private long errors;
  private long packetsIn;
  private long packetsOut;
  private long bytesIn;
  private long bytesOut;
}
