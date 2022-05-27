package io.mapsmessaging.network.protocol.impl.semtech.handlers;

import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.VERSION;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.protocol.impl.semtech.SemTechProtocol;
import io.mapsmessaging.network.protocol.impl.semtech.packet.SemTechPacket;
import io.mapsmessaging.network.protocol.impl.semtech.packet.TxAcknowledge;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class TxAckHandler extends Handler {

  @Override
  public void process(SemTechProtocol protocol, SemTechPacket packet) {
    TxAcknowledge txAck = (TxAcknowledge) packet;
    PacketHandler.getInstance().getMessageStateContext().complete(txAck.getToken());
    sendMessage(protocol, packet.getFromAddress());
    if(txAck.getJsonObject().length()>0){
      Map<String, String> meta = new LinkedHashMap<>();
      meta.put("protocol", "SemTech");
      meta.put("version", "" + VERSION);
      meta.put("time_ms", "" + System.currentTimeMillis());
      MessageBuilder builder = new MessageBuilder();
      builder.setOpaqueData(txAck.getJsonObject().getBytes(StandardCharsets.UTF_8));
      builder.setMeta(meta);
      Message message = builder.build();
      try {
        protocol.getInbound().storeMessage(message);
      } catch (IOException e) {
        e.printStackTrace();
      }

    }
  }

}
