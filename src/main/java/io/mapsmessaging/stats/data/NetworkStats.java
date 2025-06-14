package io.mapsmessaging.stats.data;

import lombok.Data;

@Data
public class NetworkStats {
  private int interfaceCount;
  private int activeInterfaceCount;
}
