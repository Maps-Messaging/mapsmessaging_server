package io.mapsmessaging.stats.data;

import lombok.Data;

@Data
public class ServerStats {
  private String serverId;
  private String serverName;
  private String licenseId;
  private long timestamp;

  private ConnectionStats connections;
  private MemoryStats memory;
  private VersionInfo version;
  private DiskStats disk;
  private CpuStats cpu;
  private UptimeStats uptime;
  private NetworkStats network;
}
