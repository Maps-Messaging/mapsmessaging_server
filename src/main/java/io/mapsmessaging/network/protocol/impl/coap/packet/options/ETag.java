package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class ETag extends Option{

  @Getter
  private final List<byte[]> etags;

  public ETag() {
    super(Constants.ETAG);
    etags = new ArrayList<>();
  }

  @Override
  public void update(byte[] value) throws IOException {
    etags.add(value);
  }
}
