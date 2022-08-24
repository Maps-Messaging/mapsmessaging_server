package io.mapsmessaging.network.protocol.impl.coap;

import static io.mapsmessaging.logging.ServerLogMessages.COAP_CLOSED;
import static io.mapsmessaging.logging.ServerLogMessages.COAP_CREATED;
import static io.mapsmessaging.logging.ServerLogMessages.COAP_FAILED_TO_PROCESS;
import static io.mapsmessaging.logging.ServerLogMessages.COAP_FAILED_TO_SEND;
import static io.mapsmessaging.logging.ServerLogMessages.COAP_PACKET_SENT;
import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.BLOCK2;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.session.SessionContext;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.coap.listeners.Listener;
import io.mapsmessaging.network.protocol.impl.coap.listeners.ListenerFactory;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.BlockWiseSend;
import io.mapsmessaging.network.protocol.impl.coap.packet.Code;
import io.mapsmessaging.network.protocol.impl.coap.packet.PacketFactory;
import io.mapsmessaging.network.protocol.impl.coap.packet.TYPE;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Block;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.ContentFormat;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.ETag;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Format;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.MaxAge;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Observe;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.OptionSet;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.UriPath;
import io.mapsmessaging.network.protocol.impl.coap.subscriptions.Context;
import io.mapsmessaging.network.protocol.impl.coap.subscriptions.SubscriptionState;
import io.mapsmessaging.network.protocol.impl.coap.subscriptions.TransactionState;
import io.mapsmessaging.schemas.config.SchemaConfig;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import javax.security.auth.login.LoginException;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class CoapProtocol extends ProtocolImpl {

  @Getter
  private final Logger logger;
  private final ListenerFactory listenerFactory;
  private final PacketFactory packetFactory;

  @Getter
  private final Session session;

  @Getter
  private final TransactionState transactionState;

  @Getter
  private final SubscriptionState subscriptionState;

  private final PacketPipeline outboundPipeline;
  private final DuplicationManager duplicationManager;
  private final AtomicLong messageId;
  private final SocketAddress socketAddress;
  private final CoapInterfaceManager coapInterfaceManager;
  private boolean isClosed;

  protected CoapProtocol(@NonNull @NotNull EndPoint endPoint, @NonNull @NotNull CoapInterfaceManager coapInterfaceManager, @NonNull @NotNull SocketAddress socketAddress) throws LoginException, IOException {
    super(endPoint);
    logger = LoggerFactory.getLogger(CoapProtocol.class);
    isClosed = false;
    listenerFactory = new ListenerFactory();
    packetFactory = new PacketFactory();
    messageId = new AtomicLong(System.nanoTime());
    subscriptionState = new SubscriptionState();
    transactionState = new TransactionState();
    outboundPipeline = new PacketPipeline(this);
    duplicationManager = new DuplicationManager(1);
    this.socketAddress = socketAddress;
    this.coapInterfaceManager = coapInterfaceManager;
    SessionContext context = new SessionContext(endPoint.getName(), this);
    context.setPersistentSession(false);
    context.setDuration(120);
    session = SessionManager.getInstance().create(context, this);
    session.start();
    logger.log(COAP_CREATED, socketAddress);
  }

  @Override
  public void close() throws IOException {
    if(isClosed){
      return;
    }
    logger.log(COAP_CLOSED, socketAddress);

    isClosed = true;
    SessionManager.getInstance().close(session, true);
    outboundPipeline.close();
    coapInterfaceManager.close(socketAddress);
    super.close();
  }

  public int getNextMessageId(){
    return (int) (messageId.incrementAndGet() & 0xffff);
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    Context context = subscriptionState.find(messageEvent.getDestinationName());
    if (context == null)
      return;
    boolean doBlockwise = false;
    int blockSize = 0;
    if(context.getRequest().getOptions().hasOption(BLOCK2)){
      Block block = (Block) context.getRequest().getOptions().getOption(BLOCK2);
      blockSize = 1<<(block.getSizeEx()+4);
      doBlockwise = blockSize < messageEvent.getMessage().getOpaqueData().length;
    }
    BasePacket response = context.getRequest().buildUpdatePacket(Code.CONTENT);
    if(doBlockwise){
      response = new BlockWiseSend(response);
      ((BlockWiseSend)response).setBlockSize(blockSize);
    }
    response.setFromAddress(context.getRequest().getFromAddress());
    response.setPayload(messageEvent.getMessage().getOpaqueData());
    setOptions(messageEvent, context, response.getOptions());
    if (context.getRequest().getType().equals(TYPE.NON)) {
      response.setMessageId(context.getRequest().getMessageId());
    } else {
      response.setMessageId((int) (messageId.incrementAndGet() & 0xffff));
    }
    try {
      transactionState.sent(context.getRequest().getToken(), response.getMessageId(), messageEvent.getMessage().getIdentifier(), messageEvent.getSubscription());
      outboundPipeline.send(response);
    } catch (IOException e) {
      try {
        logger.log(COAP_FAILED_TO_SEND, response.getFromAddress(), e);
        close();
      } catch (IOException ex) {
        // Ignore, we are closing currently
      }
    }
    if (!context.isObserve()) {
      subscriptionState.remove(messageEvent.getDestinationName());
      session.removeSubscription(context.getPath());
    }
  }

  private void setOptions(@NotNull @NonNull MessageEvent messageEvent, Context context, OptionSet optionSet){
    UriPath uriPath = new UriPath();
    uriPath.setPath(messageEvent.getDestinationName());
    optionSet.putOption(uriPath);
    long expiry = messageEvent.getMessage().getExpiry();
    if (expiry > 0) {
      expiry = (expiry - System.currentTimeMillis()) / 1000;
      MaxAge maxAge = new MaxAge();
      maxAge.setValue(expiry);
      optionSet.putOption(maxAge);
    }
    Map<String, TypedData> map = messageEvent.getMessage().getDataMap();
    ETag eTag = new ETag();
    for (Entry<String, TypedData> entry : map.entrySet()) {
      if (entry.getKey().toLowerCase().startsWith("etag") && entry.getValue().getData() instanceof byte[]) {
        eTag.update((byte[]) entry.getValue().getData());
      }
    }
    if (!eTag.getList().isEmpty()) {
      optionSet.add(eTag);
    }
    if (context.isObserve()) {
      Observe option = new Observe((int) (messageEvent.getMessage().getIdentifier() % 0xffff));
      optionSet.putOption(option);
    }
    List<SchemaConfig> schemas = SchemaManager.getInstance().getSchemaByContext(messageEvent.getDestinationName());
    ContentFormat contentFormat = new ContentFormat(Format.TEXT_PLAIN);
    if (!schemas.isEmpty()) {
      String mime = schemas.get(0).getMimeType();
      if (mime != null) {
        Format format = Format.stringValueOf(mime);
        contentFormat = new ContentFormat(format);
      }
    }
    optionSet.putOption(contentFormat);
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) {
    try {
      BasePacket basePacket = packetFactory.parseFrame(packet);
      if (basePacket != null) {
        logger.log(COAP_PACKET_SENT, basePacket, packet.getFromAddress());
        if(basePacket.getType() == TYPE.RST){
          close();
          return false;
        }
        BasePacket duplicateResponse = duplicationManager.getResponse(basePacket.getMessageId());
        if(duplicateResponse != null){
          outboundPipeline.send(duplicateResponse);
        }
        else {
          callListener(listenerFactory.getListener(basePacket.getId()), basePacket);
        }
      }
    }
    catch(IOException ex){
      logger.log(COAP_FAILED_TO_PROCESS, packet.getFromAddress(), ex);
      try {
        close();
      } catch (IOException e) {
        //ignore, we are closing and this is really non-recoverable
      }
    }
    return true;
  }

  private void callListener(Listener listener, BasePacket request) throws IOException {
    if (listener != null) {
      BasePacket response;
      try {
        response = listener.handle(request, this);
        if (response != null) {
          if(request.getType().equals(TYPE.CON) || request.getType().equals(TYPE.NON)) {
            duplicationManager.put(response);
          }
          outboundPipeline.send(response);
        }
      } catch (ExecutionException e) {
        logger.log(COAP_FAILED_TO_SEND, request.getFromAddress(), e);
        close();
      } catch (InterruptedException e) {
        close();
        Thread.currentThread().interrupt();
      }
    }
  }

  public void sendResponse(BasePacket response) throws IOException {
    outboundPipeline.send(response);
    duplicationManager.put(response);
  }

  protected void send(BasePacket response) throws IOException {
    Packet responsePacket = new Packet(2048, false);
    response.packFrame(responsePacket);
    responsePacket.setFromAddress(response.getFromAddress());
    responsePacket.flip();
    endPoint.sendPacket(responsePacket);
    logger.log(COAP_PACKET_SENT, responsePacket, responsePacket.getFromAddress());
  }

  @Override
  public String getName() {
    return "CoAP";
  }

  @Override
  public String getSessionId() {
    return session.getName();
  }

  @Override
  public String getVersion() {
    return "RFC7252";
  }

  public void ack(BasePacket ackPacket) throws IOException {
    byte[] token = ackPacket.getToken();
    if(token != null) {
      transactionState.ack(ackPacket.getMessageId(), token);
    }
    outboundPipeline.ack(ackPacket);
  }
}
