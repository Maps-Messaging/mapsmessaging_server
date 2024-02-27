package io.mapsmessaging.monitor.top.formatters;

public class StringFormatter implements Formatter {

  private final int len;
  private final boolean prepend;

  public StringFormatter(int len, boolean prepend) {
    this.len = len;
    this.prepend = prepend;
  }

  public String format(Object value) {
    return pad(value.toString(), len, prepend);
  }
}
