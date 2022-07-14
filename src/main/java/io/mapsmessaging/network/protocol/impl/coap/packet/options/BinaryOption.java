package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import java.io.IOException;
import lombok.Getter;

public class BinaryOption extends Option {

  @Getter
  private long value;

  public BinaryOption(int id) {
    super(id);
  }

  @Override
  public void update(byte[] data) throws IOException {
    value = 0; // reset it
    for (int x = 0; x < data.length; x++) {
      value += (long) (data[(data.length - x) - 1] & 0xFF) << (x * 8);
    }
  }
}
