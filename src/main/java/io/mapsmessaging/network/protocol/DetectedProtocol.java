package io.mapsmessaging.network.protocol;

import io.mapsmessaging.network.protocol.impl.proxy.ProxyProtocolInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DetectedProtocol {
  private ProxyProtocolInfo proxyProtocolInfo;
  private ProtocolImplFactory protocolImplFactory;
}
