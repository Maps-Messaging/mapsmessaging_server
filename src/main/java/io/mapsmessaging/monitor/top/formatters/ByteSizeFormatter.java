package io.mapsmessaging.monitor.top.formatters;

public class ByteSizeFormatter extends SizeFormatter {

  @Override
  public String format(Object value) {
    String val = super.format(value);
    if (val != null) {
      return val + "B";
    }
    return null;
  }
}
