package io.mapsmessaging.network.protocol.impl.nats.frames;

import io.mapsmessaging.network.io.Packet;
import lombok.ToString;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Parses the incoming NATS PONG frame from client or server.
 */
@ToString
public class PongFrame extends NatsFrame {

  public PongFrame() {
    super();
  }

  @Override
  public void parseFrame(Packet packet) throws IOException {
  }

  @Override
  public byte[] getCommand() {
    return "PONG".getBytes(StandardCharsets.US_ASCII);
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
