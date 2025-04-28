package io.mapsmessaging.network.protocol.impl.nats.frames;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Parses the incoming NATS PING frame from client or server.
 */
public class PingFrame extends NatsFrame {

  public PingFrame() {
    super();
  }

  @Override
  public byte[] getCommand() {
    return "PING".getBytes(StandardCharsets.US_ASCII);
  }

  @Override
  protected void parseLine(String json) {
    // No JSON content in PING frames
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public NatsFrame instance() {
    return new PingFrame();
  }

  @Override
  public SocketAddress getFromAddress() {
    return null;
  }
}
