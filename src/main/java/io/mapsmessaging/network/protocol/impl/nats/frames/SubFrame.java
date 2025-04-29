package io.mapsmessaging.network.protocol.impl.nats.frames;

import io.mapsmessaging.network.protocol.impl.nats.NatsProtocolException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Parses the incoming NATS SUB frame from client.
 */
@Getter
@Setter
@ToString
public class SubFrame extends NatsFrame {

  private String subject;
  private String subscriptionId;

  public SubFrame() {
    super();
  }

  @Override
  public byte[] getCommand() {
    return "SUB".getBytes(StandardCharsets.US_ASCII);
  }

  @Override
  protected void parseLine(String line) throws NatsProtocolException {
    String[] parts = line.trim().split(" ");
    if (parts.length != 2) {
      throw new NatsProtocolException("Invalid SUB frame header: " + line);
    }
    subject = parts[0];
    subscriptionId = parts[1];
  }

  @Override
  public boolean isValid() {
    return subject != null && subscriptionId != null;
  }

  @Override
  public NatsFrame instance() {
    return new SubFrame();
  }

  @Override
  public SocketAddress getFromAddress() {
    return null;
  }
}
