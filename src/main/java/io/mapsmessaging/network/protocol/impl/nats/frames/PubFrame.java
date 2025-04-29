package io.mapsmessaging.network.protocol.impl.nats.frames;

import lombok.Getter;
import lombok.ToString;

import java.nio.charset.StandardCharsets;

/**
 * Parses the incoming NATS PUB frame from client.
 */
@Getter
@ToString
public class PubFrame extends PayloadFrame {

  public PubFrame(int maxBufferSize) {
    super(maxBufferSize);
  }

  @Override
  public byte[] getCommand() {
    return "PUB".getBytes(StandardCharsets.US_ASCII);
  }

  @Override
  public NatsFrame instance() {
    return new PubFrame(maxBufferSize);
  }
}
