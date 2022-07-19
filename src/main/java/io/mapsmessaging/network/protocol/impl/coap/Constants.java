package io.mapsmessaging.network.protocol.impl.coap;

/*
                   +-------------------+---------------+
                   | name              | default value |
                   +-------------------+---------------+
                   | ACK_TIMEOUT       | 2 seconds     |
                   | ACK_RANDOM_FACTOR | 1.5           |
                   | MAX_RETRANSMIT    | 4             |
                   | NSTART            | 1             |
                   | DEFAULT_LEISURE   | 5 seconds     |
                   | PROBING_RATE      | 1 byte/second |
                   +-------------------+---------------+
 */
public class Constants {

  // Timeouts
  public static final long ACK_TIMEOUT = 2;
  public static final int DEFAULT_LEISURE = 5;

  public static final int PROBING_RATE = 1;

  public static final float ACK_RANDOM_FACTOR = 1.5f;

  // Counters
  public static final int MAX_RETRANSMIT = 4;
  public static final int NSTART = 1;

  private Constants() {
  }
}
