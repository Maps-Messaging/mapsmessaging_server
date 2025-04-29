package io.mapsmessaging.network.protocol.impl.nats.frames;

import lombok.ToString;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Parses the incoming NATS +OK frame from server.
 */
@ToString
public class OkFrame extends NatsFrame {

  public OkFrame() {
    super();
  }

  @Override
  public byte[] getCommand() {
    return "+OK".getBytes(StandardCharsets.US_ASCII);
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
