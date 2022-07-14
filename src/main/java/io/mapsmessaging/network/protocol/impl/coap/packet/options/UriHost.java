package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import java.io.IOException;
import lombok.Getter;
import lombok.Setter;

public class UriHost extends Option {

  @Getter
  @Setter
  private String host;

  public UriHost(){
    super(Constants.URI_HOST);
  }

  @Override
  public void update(byte[] value) throws IOException {
    host = new String(value);
  }
}
