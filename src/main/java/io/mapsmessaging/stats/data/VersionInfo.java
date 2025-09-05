package io.mapsmessaging.stats.data;

import lombok.Data;

@Data
public class VersionInfo {
  private String serverVersion;
  private String osName;
  private String osVersion;
  private String osArch;
  private String jvmVersion;
  private String jvmVendor;
  private String hostname;
}

