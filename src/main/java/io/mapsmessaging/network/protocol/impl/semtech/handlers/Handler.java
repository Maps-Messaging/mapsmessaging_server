package io.mapsmessaging.network.protocol.impl.semtech.handlers;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.protocol.impl.semtech.GatewayInfo;
import io.mapsmessaging.network.protocol.impl.semtech.SemTechProtocol;
import io.mapsmessaging.network.protocol.impl.semtech.packet.PullResponse;
import io.mapsmessaging.network.protocol.impl.semtech.packet.SemTechPacket;
import java.net.SocketAddress;
import java.security.SecureRandom;
import java.util.Random;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class Handler {

  private static final Random TokenGenerator = new SecureRandom();

  abstract void process(@NotNull @NonNull SemTechProtocol protocol,@NotNull @NonNull SemTechPacket packet);

  public void sendMessage(SemTechProtocol protocol, GatewayInfo info, SocketAddress socketAddress) {
    MessageEvent messageEvent = info.getWaitingMessages().poll();
    if(messageEvent != null) {
      byte[] raw = messageEvent.getMessage().getOpaqueData();
      if (protocol.getTransformation() != null) {
        raw = protocol.getTransformation().outgoing(messageEvent.getMessage());
      }
      try {
        JSONObject jsonObject = new JSONObject(new String(raw));
        int token = nextToken();
        PacketHandler.getInstance().getMessageStateContext().push(token, messageEvent);
        PullResponse pullResponse = new PullResponse(token, raw, socketAddress);
        protocol.sendPacket(pullResponse);
        protocol.getLogger().log(ServerLogMessages.SEMTECH_SENDING_PACKET, messageEvent.getMessage());
      } catch (JSONException e) {
        messageEvent.getCompletionTask().run();
      }
    }
  }

  private static int nextToken(){
    return (TokenGenerator.nextInt() % 0xffff);
  }
}
