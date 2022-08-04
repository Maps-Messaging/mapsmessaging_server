package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class ListOption extends Option{

  @Getter
  private final List<byte[]> list;

  public ListOption(int id) {
    super(id);
    list = new ArrayList<>();
  }

  @Override
  public void update(byte[] value) {
    list.add(value);
  }

  @Override
  public byte[] pack() {
    return new byte[0];
  }
}
