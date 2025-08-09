package io.mapsmessaging.network.protocol.impl.orbcomm.modem.device;

import io.mapsmessaging.network.io.Packet;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

/**
 * Pull-style line buffer with a state check.
 * Feed Packets; drain with hasLines()/pollLine().
 */
public final class PacketLineQueue {
  private final Charset charset;
  private final StringBuilder buf = new StringBuilder();
  private final Queue<String> lines = new ArrayDeque<>();

  public PacketLineQueue() {
    this(StandardCharsets.US_ASCII);
  }

  public PacketLineQueue(Charset charset) {
    this.charset = Objects.requireNonNull(charset, "charset");
  }

  public void feed(Packet packet) {
    byte[] data = new byte[packet.getRawBuffer().remaining()];
    packet.get(data);
    String s = new String(data, charset);
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '\r') continue;
      if (c == '\n') {
        lines.add(buf.length() == 0 ? "" : buf.toString());
        buf.setLength(0);
      } else {
        buf.append(c);
      }
    }
  }

  public boolean hasLines() {
    return !lines.isEmpty();
  }

  public String pollLine() {
    return lines.poll();
  }

  public String peekLine() {
    return lines.peek();
  }

  /**
   * True if there is an unterminated partial line buffered.
   */
  public boolean hasPartial() {
    return buf.length() > 0;
  }

  public void clear() {
    lines.clear();
    buf.setLength(0);
  }
}
