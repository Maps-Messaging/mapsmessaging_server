package io.mapsmessaging.app.top.formatters;

public class UptimeFormatter implements Formatter {

  private final int len;
  private final boolean prepend;

  public UptimeFormatter(int len, boolean prepend) {
    this.len = len;
    this.prepend = prepend;
  }

  public String format(Object value) {
    if (value instanceof Long) {
      return pad(formatUptime((Long) value), len, prepend);
    }
    return "";
  }

  private String formatUptime(long uptimeMillis) {
    long days = uptimeMillis / (24L * 60L * 60L * 1000L);
    long uptimeSeconds = uptimeMillis / 1000;
    long hours = (uptimeSeconds / 3600) % 24;
    long minutes = (uptimeSeconds % 3600) / 60;
    long seconds = uptimeSeconds % 60;
    String val = String.format("%02d:%02d:%02d", hours, minutes, seconds);
    if (days != 0) {
      val = val + " " + days + " days";
    }
    return val;
  }
}
