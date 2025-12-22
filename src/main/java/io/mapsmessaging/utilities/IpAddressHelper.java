package io.mapsmessaging.utilities;

public class IpAddressHelper {
  public static String normalizeIp(String raw) {
    if (raw == null || raw.isBlank()) {
      return "unknown";
    }

    String ip = raw.trim();

    // Strip leading slash
    if (ip.startsWith("/")) {
      ip = ip.substring(1);
    }

    // IPv6 with port: [::1]:1234
    if (ip.startsWith("[") && ip.contains("]")) {
      return ip.substring(1, ip.indexOf(']'));
    }

    // IPv4 with port: 1.2.3.4:5678
    int colon = ip.lastIndexOf(':');
    if (colon > 0 && ip.indexOf('.') != -1) {
      ip = ip.substring(0, colon);
    }

    return ip;
  }
}
