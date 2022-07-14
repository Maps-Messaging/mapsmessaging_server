package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import java.io.IOException;
import lombok.Getter;
import lombok.Setter;

public class ProxyScheme extends Option {

  @Getter
  @Setter
  private String proxyScheme;

  public ProxyScheme(){
    super(Constants.PROXY_SCHEME);
  }

  @Override
  public void update(byte[] value) throws IOException {
    proxyScheme = new String(value);
  }
}
