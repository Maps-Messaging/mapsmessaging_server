package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class IfNoneMatch extends Option {

  @Getter
  private final List<byte[]> etags;

  public IfNoneMatch() {
    super(Constants.IF_NONE_MATCH);
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
