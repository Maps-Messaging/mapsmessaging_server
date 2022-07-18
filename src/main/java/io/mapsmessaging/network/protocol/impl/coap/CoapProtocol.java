package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.engine.schema.SchemaManager;
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
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Observe;
import io.mapsmessaging.network.protocol.impl.coap.subscriptions.Context;
import io.mapsmessaging.network.protocol.impl.coap.subscriptions.SubscriptionState;
import io.mapsmessaging.network.protocol.impl.coap.subscriptions.TransactionState;
import io.mapsmessaging.schemas.config.SchemaConfig;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
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
  private final TransactionState transactionState;

  @Getter
  private final SubscriptionState subscriptionState;

  private final AtomicLong messageId;

  protected CoapProtocol(@NonNull @NotNull EndPoint endPoint) throws LoginException, IOException {
    super(endPoint);
    listenerFactory = new ListenerFactory();
    packetFactory = new PacketFactory();
    messageId = new AtomicLong(System.nanoTime());
    subscriptionState = new SubscriptionState();
    transactionState = new TransactionState();

    SessionContext context = new SessionContext(endPoint.getName(), this);
    context.setPersistentSession(false);
    context.setDuration(120);
    session = SessionManager.getInstance().create(context, this);
    session.start();
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    Context context = subscriptionState.find(messageEvent.getDestinationName());
    if(context != null){
      BasePacket response = context.getRequest().buildAckResponse(Code.CONTENT);
      response.setType(TYPE.CON);
      response.setPayload(messageEvent.getMessage().getOpaqueData());
      response.setMessageId((int)(messageId.incrementAndGet() & 0xffff));
      List<SchemaConfig> schemas = SchemaManager.getInstance().getSchemaByContext(messageEvent.getDestinationName());
      ContentFormat contentFormat = new ContentFormat(Format.TEXT_PLAIN);
      if(!schemas.isEmpty()){
        String mime = schemas.get(0).getMimeType();
        Format format = Format.stringValueOf(mime);
        contentFormat = new ContentFormat(format);
      }
      if(context.isObserve()){
        Observe option = new Observe(0);
        response.getOptions().putOption(option);
      }
      response.getOptions().putOption(contentFormat);
      try {
        transactionState.sent(context.getRequest().getToken(), messageEvent.getMessage().getIdentifier(), messageEvent.getSubscription());
        sendResponse(response, context.getRequest().getFromAddress());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      if(!context.isObserve()) {
        subscriptionState.remove(messageEvent.getDestinationName());
        session.removeSubscription(context.getPath());
      }
    }
    else{
      System.err.println("No Context Found");
    }
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) {
    try {
      BasePacket basePacket = packetFactory.parseFrame(packet);
      if (basePacket != null) {
        callListener(listenerFactory.getListener(basePacket.getId()), basePacket, packet);
      }
    }
    catch(IOException ex){
      try {
        close();
      } catch (IOException e) {

      }
    }
    return true;
  }

  private void callListener( Listener listener, BasePacket basePacket, Packet packet) throws IOException {
    if (listener != null) {
      BasePacket response;
      try {
        response = listener.handle(basePacket, this);
        if (response != null) {
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

  public void sendResponse(BasePacket response, SocketAddress fromAddress) throws IOException {
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

  public void ackToken(byte[] token) {
    transactionState.ack(token);
  }
}
