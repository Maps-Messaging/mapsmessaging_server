package io.mapsmessaging.network.protocol.impl.nats.frames;

import lombok.Getter;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Parses the incoming NATS MSG frame from server.
 */
@Getter
public class MsgFrame extends PayloadFrame {

  public MsgFrame(int maxBufferSize) {
    super(maxBufferSize);
  }

  @Override
  public byte[] getCommand() {
    return "MSG".getBytes(StandardCharsets.US_ASCII);
  }

  @Override
  public NatsFrame instance() {
    return new MsgFrame(maxBufferSize);
  }

  @Override
  public SocketAddress getFromAddress() {
    return null;
  }
}
