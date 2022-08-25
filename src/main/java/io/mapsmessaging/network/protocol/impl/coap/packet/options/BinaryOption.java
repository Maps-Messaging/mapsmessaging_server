package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import java.io.IOException;
import lombok.Getter;
import lombok.Setter;

public class BinaryOption extends Option {

  @Getter
  @Setter
  protected long value = 0;

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

  @Override
  public byte[] pack() {
    long t = value;
    int x = 0;
    byte[] buffer = new byte[8];
    int idx=7;
    while(t != 0){
      buffer[idx-x] = (byte)(t & 0xff);
      t = t >> 8;
      x++;
    }
    byte[] response = new byte[x];
    System.arraycopy(buffer, 8-x, response, 0, x);
    return response;
  }
}
