package io.mapsmessaging.network.protocol.impl.coap.listeners;

import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;

public class PutListener extends Listener {

  @Override
  public BasePacket handle(BasePacket request, CoapProtocol protocol) {
    super.publishMessage(request, protocol);
    return null;
  }
}