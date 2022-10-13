package io.mapsmessaging.network.protocol.impl.semtech.handlers;

import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.VERSION;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.protocol.impl.semtech.GatewayInfo;
import io.mapsmessaging.network.protocol.impl.semtech.SemTechProtocol;
import io.mapsmessaging.network.protocol.impl.semtech.packet.PushAck;
import io.mapsmessaging.network.protocol.impl.semtech.packet.PushData;
import io.mapsmessaging.network.protocol.impl.semtech.packet.SemTechPacket;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

public class PushDataHandler extends Handler {

  @Override
  public void process(@NotNull @NonNull SemTechProtocol protocol, @NotNull @NonNull SemTechPacket packet) {
    PushData pushData = (PushData) packet;
    if (pushData.isValid()) {
      protocol.sendPacket(new PushAck(pushData.getToken(), packet.getFromAddress()));
      if (pushData.getJsonObject() != null && pushData.getJsonObject().length() > 0) {
        try {
          JSONObject jsonObject = new JSONObject(pushData.getJsonObject());
          boolean status = jsonObject.has("stat");
          // At this point we know it is a valid packet with a valid JSON payload, so now lets process it
          Map<String, String> meta = new LinkedHashMap<>();
          meta.put("protocol", "SemTech");
          meta.put("version", "" + VERSION);
          meta.put("time_ms", "" + System.currentTimeMillis());
          MessageBuilder builder = new MessageBuilder();
          builder.setOpaqueData(pushData.getJsonObject().getBytes(StandardCharsets.UTF_8));
          builder.setMeta(meta);
          Message message = builder.build();
          GatewayInfo info = protocol.getGatewayManager().getInfo(pushData.getGatewayIdentifier());
          if (info != null) {
            if(status){
              info.getStatus().storeMessage(message);
            }
            else {
              info.getInbound().storeMessage(message);
            }
          }
        } catch (JSONException | IOException jsonParseException) {
          // Catch & ignore
        }
      }
    }
  }
}
