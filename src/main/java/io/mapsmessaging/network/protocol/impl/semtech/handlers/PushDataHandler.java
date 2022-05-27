package io.mapsmessaging.network.protocol.impl.semtech.handlers;

import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.VERSION;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.protocol.impl.semtech.SemTechProtocol;
import io.mapsmessaging.network.protocol.impl.semtech.packet.PushAck;
import io.mapsmessaging.network.protocol.impl.semtech.packet.PushData;
import io.mapsmessaging.network.protocol.impl.semtech.packet.SemTechPacket;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class PushDataHandler extends Handler {

  @Override
  public void process(SemTechProtocol protocol, SemTechPacket packet) {
    PushData pushData = (PushData) packet;
    if (pushData.isValid()) {
      try {
        JSONObject jsonObject = new JSONObject(pushData.getJsonObject());
        protocol.sendPacket(new PushAck(pushData.getToken(), packet.getFromAddress()));
        // At this point we know it is a valid packet with a valid JSON payload, so now lets process it
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("protocol", "SemTech");
        meta.put("version", "" + VERSION);
        meta.put("time_ms", "" + System.currentTimeMillis());
        MessageBuilder builder = new MessageBuilder();
        builder.setOpaqueData(pushData.getJsonObject().getBytes(StandardCharsets.UTF_8));
        builder.setMeta(meta);
        Message message = builder.build();
        System.err.println("Received inbound message : "+message);
        protocol.getInbound().storeMessage(message);
      } catch (JSONException | IOException jsonParseException) {
        jsonParseException.printStackTrace();
      }
    }
  }
}
