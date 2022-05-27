package io.mapsmessaging.network.protocol.impl.semtech.handlers;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.network.protocol.impl.semtech.SemTechProtocol;
import io.mapsmessaging.network.protocol.impl.semtech.packet.PullResponse;
import io.mapsmessaging.network.protocol.impl.semtech.packet.SemTechPacket;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class Handler {

  private static AtomicInteger TOKEN = new AtomicInteger();


  abstract void process(@NotNull @NonNull SemTechProtocol protocol,@NotNull @NonNull SemTechPacket packet);

  public void sendMessage(SemTechProtocol protocol, SocketAddress socketAddress) {
    MessageEvent messageEvent = protocol.getWaitingMessages().poll();
    byte[] raw = messageEvent.getMessage().getOpaqueData();
    try {
      JSONObject jsonObject = new JSONObject(new String(raw));
      int token = TOKEN.incrementAndGet()%0x7FFF;
      PacketHandler.getInstance().getMessageStateContext().push(token, messageEvent);
      PullResponse pullResponse = new PullResponse(token, raw, socketAddress);
      protocol.sendPacket(pullResponse);
    } catch (JSONException e) {
      messageEvent.getCompletionTask().run();
    }
  }


}
