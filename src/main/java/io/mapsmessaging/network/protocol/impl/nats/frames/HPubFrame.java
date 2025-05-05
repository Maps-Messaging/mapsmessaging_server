package io.mapsmessaging.network.protocol.impl.nats.frames;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.nio.charset.StandardCharsets;

@Getter
@Setter
@ToString
public class HPubFrame extends HPayloadFrame {

  protected HPubFrame(int maxBufferSize) {
    super(maxBufferSize);
  }

  @Override
  public PayloadFrame duplicate() {
    return copy(new HPubFrame(maxBufferSize));
  }

  @Override
  public byte[] getCommand() {
    return "HPUB".getBytes(StandardCharsets.US_ASCII);
  }

  @Override
  public NatsFrame instance() {
    return new HPubFrame(maxBufferSize);
  }

}
