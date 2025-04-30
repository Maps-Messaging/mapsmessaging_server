package io.mapsmessaging.network.protocol.impl.nats.frames;

import java.nio.charset.StandardCharsets;

public class HMsgFrame extends HPayloadFrame {

  public HMsgFrame(int maxBufferSize) {
    super(maxBufferSize);
  }


  @Override
  public byte[] getCommand() {
    return "HMSG".getBytes(StandardCharsets.US_ASCII);
  }

  @Override
  public NatsFrame instance() {
    return new HMsgFrame(maxBufferSize);
  }

}
