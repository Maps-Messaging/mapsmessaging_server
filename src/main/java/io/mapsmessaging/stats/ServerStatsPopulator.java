package io.mapsmessaging.stats;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.network.NetworkManager;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.stats.data.*;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class ServerStatsPopulator {

  private static final OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

  public static ServerStats collect(String serverId, String serverName, String licenseId, String serverVersion, long serverUptimeSecs) {
    ServerStats stats = new ServerStats();
    stats.setServerName(serverName);
    stats.setServerId(serverId);
    stats.setLicenseId(licenseId);
    stats.setTimestamp(System.currentTimeMillis());

    stats.setVersion(buildVersionInfo(serverVersion));
    stats.setMemory(buildMemoryStats());
    stats.setDisk(buildDiskStats());
    stats.setCpu(buildCpuStats());
    stats.setUptime(buildUptimeStats(serverUptimeSecs));
    stats.setNetwork(buildNetworkStats());
    stats.setConnections(buildConnectionStats());

    return stats;
  }

  private static VersionInfo buildVersionInfo(String serverVersion) {
    VersionInfo info = new VersionInfo();
    info.setServerVersion(serverVersion);
    info.setOsName(System.getProperty("os.name"));
    info.setOsVersion(System.getProperty("os.version"));
    info.setOsArch(System.getProperty("os.arch"));
    info.setJvmVersion(System.getProperty("java.version"));
    info.setJvmVendor(System.getProperty("java.vendor"));
    try {
      info.setHostname(InetAddress.getLocalHost().getHostName());
    } catch (Exception e) {
      info.setHostname("unknown");
    }
    return info;
  }

  private static long getNonHeapUsedMb() {
    long nonHeapUsedBytes = 0;
    for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
      if (pool.getType() == MemoryType.NON_HEAP) {
        nonHeapUsedBytes += pool.getUsage().getUsed();
      }
    }
    return nonHeapUsedBytes / (1024 * 1024);
  }

  private static MemoryStats buildMemoryStats() {
    Runtime runtime = Runtime.getRuntime();
    MemoryStats mem = new MemoryStats();

    mem.setHeapUsedMb((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
    mem.setNonHeapUsedMb(getNonHeapUsedMb());
    mem.setTotalJvmMemoryMb(runtime.totalMemory() / (1024 * 1024));

    com.sun.management.OperatingSystemMXBean os =
        (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    mem.setFreePhysicalMemoryMb(os.getFreeMemorySize() / (1024 * 1024));
    mem.setTotalPhysicalMemoryMb(os.getTotalMemorySize() / (1024 * 1024));

    mem.setSwapTotalMb(os.getTotalSwapSpaceSize() / (1024 * 1024));
    mem.setSwapFreeMb(os.getFreeSwapSpaceSize() / (1024 * 1024));
    mem.setVirtualTotalMb((os.getTotalMemorySize() + os.getTotalSwapSpaceSize()) / (1024 * 1024));
    mem.setVirtualFreeMb((os.getFreeMemorySize() + os.getFreeSwapSpaceSize()) / (1024 * 1024));

    return mem;
  }


  private static DiskStats buildDiskStats() {
    String mapsHome = System.getenv("MAPS_HOME");
    if (mapsHome == null) mapsHome = "."; // fallback

    File mapsDir = new File(mapsHome).getAbsoluteFile();
    while (mapsDir != null && !mapsDir.exists()) {
      mapsDir = mapsDir.getParentFile();
    }
    File rootMount = mapsDir;

    DiskStats disk = new DiskStats();
    if(rootMount != null) {
      disk.setTotalDiskMb(rootMount.getTotalSpace() / (1024 * 1024));
      disk.setFreeDiskMb(rootMount.getFreeSpace() / (1024 * 1024));
    }
    return disk;
  }

  private static CpuStats buildCpuStats() {
    CpuStats cpu = new CpuStats();
    if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
      com.sun.management.OperatingSystemMXBean extended = (com.sun.management.OperatingSystemMXBean) osBean;
      cpu.setProcessCpuLoadPercent(extended.getProcessCpuLoad() * 100);
      cpu.setSystemCpuLoadPercent(extended.getSystemCpuLoad() * 100);
      cpu.setProcessCpuTimeMillis(extended.getProcessCpuTime() / 1_000_000);
    }
    return cpu;
  }

  private static UptimeStats buildUptimeStats(long serverUptimeSecs) {
    UptimeStats up = new UptimeStats();
    up.setServerUptimeSecs(serverUptimeSecs);
    up.setSystemUptimeSecs((System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime()) / 1000);
    return up;
  }

  private static NetworkStats buildNetworkStats() {
    NetworkStats net = new NetworkStats();
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      int total = 0;
      int active = 0;
      while (interfaces.hasMoreElements()) {
        NetworkInterface ni = interfaces.nextElement();
        total++;
        if (ni.isUp() && !ni.isLoopback()) active++;
      }
      net.setInterfaceCount(total);
      net.setActiveInterfaceCount(active);
    } catch (SocketException e) {
      net.setInterfaceCount(0);
      net.setActiveInterfaceCount(0);
    }
    return net;
  }

  private static ConnectionStats buildConnectionStats() {
    ConnectionStats conn = new ConnectionStats();
    MessageDaemon daemon = MessageDaemon.getInstance();
    NetworkManager networkManager = daemon.getSubSystemManager().getNetworkManager();
    long totalErrors = EndPointServerStatus.SystemTotalFailedConnections.sum();
    int connections = 0;
    for (EndPointManager manager : networkManager.getAll()) {
      connections += manager.getEndPointServer().getActiveEndPoints().size();
      totalErrors += manager.getEndPointServer().getTotalErrors();
    }


    // Stubbed values — replace with real server stats
    conn.setCurrentConnections(connections);
    conn.setErrors(totalErrors);
    conn.setPacketsIn(EndPointServerStatus.SystemTotalPacketsReceived.sum());
    conn.setPacketsOut(EndPointServerStatus.SystemTotalPacketsSent.sum());
    conn.setBytesIn(EndPointServerStatus.SystemTotalBytesReceived.sum());
    conn.setBytesOut(EndPointServerStatus.SystemTotalBytesSent.sum());
    return conn;
  }

  private ServerStatsPopulator(){}
}

