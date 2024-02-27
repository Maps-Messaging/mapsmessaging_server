package io.mapsmessaging.monitor.top.formatters;

public class DecimalSizeFormatter implements Formatter {

  private static final long K = 1000;
  private static final long M = K * 100;
  private static final long G = M * 1000;
  private static final long T = G * 1000;

  private final int len;

  public DecimalSizeFormatter(int len) {
    this.len = len;
  }

  @Override
  public String format(Object value) {
    if (value instanceof Number) {
      long val = ((Number) value).longValue();
      return pad(formatSize(val), len, false);
    }
    return null;
  }


  public static String formatSize(long bytes) {
    if (bytes >= T) {
      return String.format("%d T", (int) (bytes / (double) T));
    } else if (bytes >= G) {
      return String.format("%d G", (int) (bytes / (double) G));
    } else if (bytes >= M) {
      return String.format("%d M", (int) (bytes / (double) M));
    } else if (bytes >= K) {
      return String.format("%d K", (int) (bytes / (double) K));
    } else {
      return "" + bytes;
    }
  }
}
