package io.mapsmessaging.network.protocol.impl.coap.listeners;

import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.Code;

public class PatchListener extends Listener {

  @Override
  public BasePacket handle(BasePacket request, CoapProtocol protocol) {
    switch(request.getType()){
      case CON:
        return request.buildAckResponse(Code.METHOD_NOT_ALLOWED);

      case NON:
      case ACK:
      case RST:
    }
    return null;
  }
}