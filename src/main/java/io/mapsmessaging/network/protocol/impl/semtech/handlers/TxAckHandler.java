package io.mapsmessaging.network.protocol.impl.semtech.handlers;

import static io.mapsmessaging.network.protocol.impl.semtech.packet.PacketFactory.VERSION;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.protocol.impl.semtech.GatewayInfo;
import io.mapsmessaging.network.protocol.impl.semtech.GatewayManager;
import io.mapsmessaging.network.protocol.impl.semtech.SemTechProtocol;
import io.mapsmessaging.network.protocol.impl.semtech.packet.SemTechPacket;
import io.mapsmessaging.network.protocol.impl.semtech.packet.TxAcknowledge;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class TxAckHandler extends Handler {

  @Override
  public void process(@NotNull @NonNull SemTechProtocol protocol, @NotNull @NonNull SemTechPacket packet) {
    TxAcknowledge txAck = (TxAcknowledge) packet;
    PacketHandler.getInstance().getMessageStateContext().complete(txAck.getToken());
    GatewayInfo info = protocol.getGatewayManager().getInfo(GatewayManager.dumpIdentifier(txAck.getGatewayIdentifier()));
    if (info != null) {
      sendMessage(protocol, info, packet.getFromAddress());
    }
    if (txAck.getJsonObject().length() > 0) {
      Map<String, String> meta = new LinkedHashMap<>();
      meta.put("protocol", "SemTech");
      meta.put("version", "" + VERSION);
      meta.put("time_ms", "" + System.currentTimeMillis());
      MessageBuilder builder = new MessageBuilder();
      builder.setOpaqueData(txAck.getJsonObject().getBytes(StandardCharsets.UTF_8));
      builder.setMeta(meta);
      Message message = builder.build();
      try {
        if (info != null) {
          info.getInbound().storeMessage(message);
        }
      } catch (IOException e) {
        // Catch & ignore
      }
    }
  }
}
