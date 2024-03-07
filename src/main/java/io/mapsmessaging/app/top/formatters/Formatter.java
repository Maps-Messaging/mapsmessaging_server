package io.mapsmessaging.app.top.formatters;

public interface Formatter {
  String format(Object value);

  default String pad(String val, int len, boolean prepend) {
    int diff = len - val.length();
    if (diff < 0) {
      return val.substring(0, len);
    } else if (diff > 0) {
      if (prepend) {
        return String.format("%" + len + "s", val);
      } else {
        return String.format("%-" + len + "s", val);
      }
    } else {
      return val;
    }
  }
}
