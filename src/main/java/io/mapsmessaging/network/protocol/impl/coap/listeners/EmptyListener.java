package io.mapsmessaging.network.protocol.impl.coap.listeners;

import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.Empty;
import java.io.IOException;

public class EmptyListener extends Listener {

  @Override
  public BasePacket handle(BasePacket request, CoapProtocol protocol) throws IOException {
    switch (request.getType()) {
      case ACK:
        if (request.getToken() != null) {
          protocol.ack(request.getMessageId(), request.getToken());
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
        BasePacket pingResponse = new Empty(request.getMessageId());
        pingResponse.setFromAddress(request.getFromAddress());
        protocol.sendResponse(pingResponse);
        break;
      case NON:
        break;

    }
    return null;
  }
}
