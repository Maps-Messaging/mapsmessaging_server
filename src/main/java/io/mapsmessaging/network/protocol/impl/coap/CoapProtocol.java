package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.engine.session.SessionContext;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.coap.listeners.Listener;
import io.mapsmessaging.network.protocol.impl.coap.listeners.ListenerFactory;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.Code;
import io.mapsmessaging.network.protocol.impl.coap.packet.PacketFactory;
import io.mapsmessaging.network.protocol.impl.coap.packet.TYPE;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.ContentFormat;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Format;
import io.mapsmessaging.network.protocol.impl.coap.subscriptions.Context;
import io.mapsmessaging.network.protocol.impl.coap.subscriptions.SubscriptionState;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.ExecutionException;
import javax.security.auth.login.LoginException;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class CoapProtocol extends ProtocolImpl {
  private final ListenerFactory listenerFactory;
  private final PacketFactory packetFactory;

  @Getter
  private final Session session;

  @Getter
  private final SubscriptionState subscriptionState;

  protected CoapProtocol(@NonNull @NotNull EndPoint endPoint) throws LoginException, IOException {
    super(endPoint);
    listenerFactory = new ListenerFactory();
    packetFactory = new PacketFactory();
    SessionContext context = new SessionContext(endPoint.getName(), this);
    context.setPersistentSession(false);
    context.setDuration(120);
    subscriptionState = new SubscriptionState();
    session = SessionManager.getInstance().create(context, this);
    session.start();
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    Context context = subscriptionState.remove(messageEvent.getDestinationName());
    if(context != null){
      BasePacket response = context.getRequest().buildAckResponse(Code.CONTENT);
      response.setType(TYPE.CON);
      response.setPayload(messageEvent.getMessage().getOpaqueData());
      response.setMessageId((int)(System.currentTimeMillis() & 0x7fff));
      ContentFormat format = new ContentFormat(Format.TEXT_PLAIN);
      response.getOptions().putOption(format);
      try {
        sendResponse(response, context.getRequest().getFromAddress());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      session.removeSubscription(context.getPath());
    }
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    BasePacket basePacket = packetFactory.parseFrame(packet);
    if(basePacket != null){
      Listener listener = listenerFactory.getListener(basePacket.getId());
      if(listener != null){
        BasePacket response = null;
        try {
          response = listener.handle(basePacket, this);
          if(response != null) {
            sendResponse(response, packet.getFromAddress());
          }
        } catch (ExecutionException e) {
          close();
        } catch (InterruptedException e) {
          close();
          Thread.currentThread().interrupt();
        }
      }
    }
    return true;
  }

  private void sendResponse(BasePacket response, SocketAddress fromAddress) throws IOException {
    Packet responsePacket = new Packet(1024, false);
    response.packFrame(responsePacket);
    responsePacket.setFromAddress(fromAddress);
    responsePacket.flip();
    endPoint.sendPacket(responsePacket);
  }

  @Override
  public void close() throws IOException {
    SessionManager.getInstance().close(session, true);
    super.close();
  }

  @Override
  public String getName() {
    return "CoAP";
  }

  @Override
  public String getSessionId() {
    return null;
  }

  @Override
  public String getVersion() {
    return "RFC7252";
  }
}
