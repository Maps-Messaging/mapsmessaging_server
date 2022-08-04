package io.mapsmessaging.network.protocol.impl.coap.listeners;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;

public class DeleteListener extends Listener {

  @Override
  public BasePacket handle(BasePacket request, CoapProtocol protocol) {

    super.publishMessage(request, protocol, true);
    return null;
  }


  protected Message build(BasePacket request){
    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.setOpaqueData(null);
    messageBuilder.setQoS(QualityOfService.AT_MOST_ONCE);
    messageBuilder.setRetain(true);
    return messageBuilder.build();
  }
}