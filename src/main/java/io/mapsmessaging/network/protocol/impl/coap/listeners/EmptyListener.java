package io.mapsmessaging.network.protocol.impl.coap.listeners;

import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.TYPE;

public class EmptyListener extends Listener {

  @Override
  public BasePacket handle(BasePacket request, CoapProtocol protocol) {
    if(request.getType().equals(TYPE.ACK)){
      if(request.getToken() != null){
        protocol.ackToken(request.getToken());
      }
    }
    return null;
  }
}
