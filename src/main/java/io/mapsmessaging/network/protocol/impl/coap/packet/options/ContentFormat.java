package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import java.io.IOException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class ContentFormat extends BinaryOption {

  @Getter
  @Setter
  private Format format;

  public ContentFormat() {
    super(Constants.CONTENT_FORMAT);
  }

  @Override
  public void update(byte[] data) throws IOException {
    super.update(data);
    format = Format.valueOf((int)getValue());
  }
}
