package io.mapsmessaging.network.protocol.impl.nats.frames;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Parses the incoming NATS +OK frame from server.
 */
public class OkFrame extends NatsFrame {

  public OkFrame() {
    super();
  }

  @Override
  public byte[] getCommand() {
    return "+OK".getBytes(StandardCharsets.US_ASCII);
  }

  @Override
  protected void parseLine(String json) {
    // No JSON content in +OK frames
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public NatsFrame instance() {
    return new OkFrame();
  }

  @Override
  public SocketAddress getFromAddress() {
    return null;
  }
}
