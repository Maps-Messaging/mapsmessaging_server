package io.mapsmessaging.network.protocol.impl.orbcomm.modem.messages;

public class MessageNameGenerator {

  public static String incrementString(String current) {
    long value = base36ToDecimal(current);
    return decimalToBase36(value + 1);
  }

  public static long base36ToDecimal(String input) {
    input = input.trim().toLowerCase();
    if (input.isEmpty()) return 0;

    long result = 0;
    for (char c : input.toCharArray()) {
      int digit;
      if (c >= '0' && c <= '9') {
        digit = c - '0';
      } else if (c >= 'a' && c <= 'z') {
        digit = c - 'a' + 10;
      } else {
        throw new IllegalArgumentException("Invalid char: " + c);
      }
      result = result * 36 + digit;
    }
    return result;
  }

  public static String decimalToBase36(long value) {
    if (value == 0) return "       0";

    StringBuilder sb = new StringBuilder();
    while (value > 0) {
      int rem = (int) (value % 36);
      char c = (rem < 10) ? (char) ('0' + rem) : (char) ('a' + rem - 10);
      sb.append(c);
      value /= 36;
    }
    return String.format("%8s", sb.reverse().toString());
  }

  private MessageNameGenerator() {
  }

  public static void main(String[] args) {
    String current = "";
    for (int i = 0; i < 100000; i++) {
      current = incrementString(current);
      long val = base36ToDecimal(current);
      String back = decimalToBase36(val);
      if (!current.equals(back)) {
        System.err.println("Mismatch: " + current + " -> " + val + " -> " + back);
      }
      if (i % 10000 == 0) {
        System.out.println(current + " = " + val);
      }
    }
  }
}
