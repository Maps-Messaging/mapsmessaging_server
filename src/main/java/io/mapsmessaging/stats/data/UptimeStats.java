package io.mapsmessaging.stats.data;

import lombok.Data;

@Data
public class UptimeStats {
  private long serverUptimeSecs;
  private long systemUptimeSecs; // if available
}
