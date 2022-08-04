package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class IfMatch extends Option{

  @Getter
  private final List<byte[]> etags;

  public IfMatch() {
    super(Constants.IF_MATCH);
    etags = new ArrayList<>();
  }

  @Override
  public void update(byte[] value) throws IOException {
    etags.add(value);
  }

  @Override
  public byte[] pack() {
    return new byte[0];
  }
}
