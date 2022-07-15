package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import java.io.IOException;
import lombok.Getter;
import lombok.Setter;

public class StringOption extends Option {

  @Getter
  @Setter
  private String value;

  public StringOption(int id){
    super(id);
  }

  @Override
  public void update(byte[] value) throws IOException {
    this.value = new String(value);
  }

  @Override
  public byte[] pack() {
    return value.getBytes();
  }
}