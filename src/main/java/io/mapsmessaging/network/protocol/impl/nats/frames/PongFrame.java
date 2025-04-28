package io.mapsmessaging.network.protocol.impl.nats.frames;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Parses the incoming NATS PONG frame from client or server.
 */
public class PongFrame extends NatsFrame {

  public PongFrame() {
    super();
  }

  @Override
  public byte[] getCommand() {
    return "PONG".getBytes(StandardCharsets.US_ASCII);
  }

  @Override
  protected void parseLine(String json) {
    // No JSON content in PONG frames
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public NatsFrame instance() {
    return new PongFrame();
  }

  @Override
  public SocketAddress getFromAddress() {
    return null;
  }
}
