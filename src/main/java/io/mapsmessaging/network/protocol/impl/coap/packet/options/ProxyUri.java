package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import java.io.IOException;
import lombok.Getter;
import lombok.Setter;

public class ProxyUri extends Option {

  @Getter
  @Setter
  private String proxyUri;

  public ProxyUri(){
    super(Constants.PROXY_URI);
  }

  @Override
  public void update(byte[] value) throws IOException {
    proxyUri = new String(value);
  }
}
