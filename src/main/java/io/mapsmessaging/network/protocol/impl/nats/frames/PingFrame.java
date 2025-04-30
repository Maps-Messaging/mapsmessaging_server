package io.mapsmessaging.network.protocol.impl.nats.frames;

import io.mapsmessaging.network.io.Packet;
import lombok.ToString;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Parses the incoming NATS PING frame from client or server.
 */
@ToString
public class PingFrame extends NatsFrame {

  public PingFrame() {
    super();
  }

  @Override
  public void parseFrame(Packet packet) throws IOException {
  }

  @Override
  public byte[] getCommand() {
    return "PING".getBytes(StandardCharsets.US_ASCII);
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
