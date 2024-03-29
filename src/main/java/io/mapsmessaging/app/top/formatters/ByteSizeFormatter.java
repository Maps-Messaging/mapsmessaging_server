package io.mapsmessaging.app.top.formatters;

public class ByteSizeFormatter implements Formatter {

  private static final long KB = 1024L;
  private static final long MB = KB * 1024;
  private static final long GB = MB * 1024;
  private static final long TB = GB * 1024;

  private final int len;
  private final boolean prepend;

  public ByteSizeFormatter(int len) {
    this(len, false);
  }

  public ByteSizeFormatter(int len, boolean prepend) {
    this.len = len;
    this.prepend = prepend;
  }

  @Override
  public String format(Object value) {
    if (value instanceof Number) {
      long val = ((Number) value).longValue();
      return pad(formatSize(val), len, prepend);
    }
    return null;
  }

  public static String formatSize(long bytes) {
    if (bytes >= TB) {
      return String.format("%.1f TB", bytes / (double) TB);
    } else if (bytes >= GB) {
      return String.format("%.1f GB", bytes / (double) GB);
    } else if (bytes >= MB) {
      return String.format("%.1f MB", bytes / (double) MB);
    } else if (bytes >= KB) {
      return String.format("%.1f KB", bytes / (double) KB);
    } else {
      return "" + bytes;
    }
  }
}
