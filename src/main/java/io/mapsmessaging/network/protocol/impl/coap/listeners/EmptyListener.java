package io.mapsmessaging.network.protocol.impl.coap.listeners;

import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;

public class EmptyListener extends Listener {

  @Override
  public BasePacket handle(BasePacket request, CoapProtocol protocol) {
    System.err.println("Unhandled request: "+request);
    return null;
  }
}
