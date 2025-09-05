package io.mapsmessaging.stats.data;

import lombok.Data;

@Data
public class DiskStats {
  private long totalDiskMb;
  private long freeDiskMb;
}

