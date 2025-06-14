package io.mapsmessaging.stats.data;

import lombok.Data;

@Data
public class MemoryStats {
  private long heapUsedMb;
  private long nonHeapUsedMb;
  private long totalJvmMemoryMb;
  private long freePhysicalMemoryMb;
  private long totalPhysicalMemoryMb;
  private long totalSwapSpaceMb;
  private long freeSwapSpaceMb;
  private long swapTotalMb;
  private long swapFreeMb;
  private long virtualTotalMb;
  private long virtualFreeMb;
}

