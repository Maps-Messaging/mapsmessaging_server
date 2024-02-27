package io.mapsmessaging.monitor.top.formatters;

public class SizeFormatter implements Formatter {

  private static final long KB = 1024L;
  private static final long MB = KB * 1024;
  private static final long GB = MB * 1024;
  private static final long TB = GB * 1024;

  @Override
  public String format(Object value) {
    if (value instanceof Number) {
      long val = ((Number) value).longValue();
      return formatSize(val);
    }
    return null;
  }


  public static String formatSize(long bytes) {
    if (bytes >= TB) {
      return String.format("%.1f T", bytes / (double) TB);
    } else if (bytes >= GB) {
      return String.format("%.1f G", bytes / (double) GB);
    } else if (bytes >= MB) {
      return String.format("%.1f M", bytes / (double) MB);
    } else if (bytes >= KB) {
      return String.format("%.1f K", bytes / (double) KB);
    } else {
      return "" + bytes;
    }
  }
}
