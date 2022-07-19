package io.mapsmessaging.network.protocol.impl.coap.listeners;

import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import java.io.IOException;

public class EmptyListener extends Listener {

  @Override
  public BasePacket handle(BasePacket request, CoapProtocol protocol) {
    switch (request.getType()) {
      case ACK:
        if (request.getToken() != null) {
          protocol.ackToken(request.getToken());
        }
        break;

      case RST:
        try {
          protocol.close();
        } catch (IOException e) {
          //
        }
        break;

      case CON:
      case NON:
        break;

    }
    return null;
  }
}
