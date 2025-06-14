package io.mapsmessaging.stats.data;

import lombok.Data;

@Data
public class CpuStats {
  private double processCpuLoadPercent;
  private double systemCpuLoadPercent;
  private long processCpuTimeMillis;
}

