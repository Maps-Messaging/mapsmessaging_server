/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.config.protocol.impl.CoapConfig;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.CoapProtocolInformation;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.session.SessionContext;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.coap.blockwise.BlockReceiveMonitor;
import io.mapsmessaging.network.protocol.impl.coap.listeners.Listener;
import io.mapsmessaging.network.protocol.impl.coap.listeners.ListenerFactory;
import io.mapsmessaging.network.protocol.impl.coap.packet.*;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.*;
import io.mapsmessaging.network.protocol.impl.coap.subscriptions.Context;
import io.mapsmessaging.network.protocol.impl.coap.subscriptions.SubscriptionState;
import io.mapsmessaging.network.protocol.impl.coap.subscriptions.TransactionState;
import io.mapsmessaging.schemas.config.SchemaResource;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import static io.mapsmessaging.logging.ServerLogMessages.*;
import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.BLOCK2;

public class CoapProtocol extends Protocol {

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
  private final AtomicLong lastAccess;

  @Getter
  private final BlockReceiveMonitor blockReceiveMonitor;
  private boolean isClosed;


  @Getter
  private final int mtu;

  private final int maxBlockSize;

  protected CoapProtocol(@NonNull @NotNull EndPoint endPoint, @NonNull @NotNull CoapInterfaceManager coapInterfaceManager, @NonNull @NotNull SocketAddress socketAddress)
      throws LoginException, IOException {
    super(endPoint, socketAddress, endPoint.getConfig().getProtocolConfig("coap"));
    lastAccess = new AtomicLong(System.currentTimeMillis());
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
    mtu = coapInterfaceManager.getMtu();
    CoapConfig coapConfig = (CoapConfig) protocolConfig;
    maxBlockSize = coapConfig.getMaxBlockSize();
    int idle = coapConfig.getIdleTime();
    keepAlive = idle * 1000L;
    blockReceiveMonitor = new BlockReceiveMonitor();

    String sessionName = socketAddress.toString();
    sessionName = sessionName.replace(":", "_");
    SessionContext context = new SessionContext(sessionName, new ProtocolClientConnection(this));
    context.setPersistentSession(false);
    context.setReceiveMaximum(5);
    session = SessionManager.getInstance().create(context, this);
    session.start();
    logger.log(COAP_CREATED, socketAddress, mtu, maxBlockSize);
  }

  @Override
  public Subject getSubject() {
    return session.getSecurityContext().getSubject();
  }

  @Override
  public void close() throws IOException {
    if (isClosed) {
      return;
    }
    logger.log(COAP_CLOSED, socketAddress);

    isClosed = true;
    SessionManager.getInstance().close(session, true);
    outboundPipeline.close();
    coapInterfaceManager.close(socketAddress);
    endPoint.getServer().handleCloseEndPoint(endPoint);
    if (mbean != null) {
      mbean.close();
    }
  }

  public int getNextMessageId(){
    return (int) (messageId.incrementAndGet() & 0xffff);
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    Context context = subscriptionState.find(messageEvent.getDestinationName());
    if (context == null) {
      return;
    }
    ParsedMessage parsedMessage = parseOutboundMessage(messageEvent);
    if(parsedMessage == null) {
      return;
    }
    byte[] data = parsedMessage.getMessage().getOpaqueData();
    endPoint.updateWriteBytes(data.length);
    endPoint.getEndPointStatus().incrementSentMessages();
    boolean doBlockwise = false;
    int blockSize = 0;
    int blockNumber = 0;
    if(context.getRequest().getOptions().hasOption(BLOCK2)){
      Block block = (Block) context.getRequest().getOptions().getOption(BLOCK2);
      blockNumber = block.getNumber();
      if(block.getSizeEx() != 0b111){
        blockSize = 1<<(block.getSizeEx()+4);
        doBlockwise = blockSize < data.length;
        logger.log(COAP_BLOCK2_REQUEST, block.getNumber(), block.getSizeEx(), block.isMore());
      }
      else{
        logger.log(COAP_BERT_NOT_SUPPORTED, socketAddress);
      }
    }
    BasePacket response = context.getRequest().buildUpdatePacket(Code.CONTENT);
    if(doBlockwise){
      response = new BlockWiseSend(response);
    }
    response.setFromAddress(context.getRequest().getFromAddress());
    response.setPayload(data);
    setOptions(messageEvent, context, response.getOptions());
    if(doBlockwise){
      ((BlockWiseSend)response).setBlockSize(blockSize);
      ((BlockWiseSend)response).setBlockNumber(blockNumber);
    }
    if (context.getRequest().getType().equals(TYPE.NON)) {
      response.setMessageId(context.getRequest().getMessageId());
    } else {
      response.setMessageId((int) (messageId.incrementAndGet() & 0xffff));
    }
    response.setCallback(messageEvent.getCompletionTask());

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
      Observe option = new Observe((int) (context.getObserveId().incrementAndGet() & 0xffff));
      optionSet.putOption(option);
    }
    List<SchemaResource> schemas = SchemaManager.getInstance().getSchemaByContext(messageEvent.getDestinationName());
    ContentFormat contentFormat = new ContentFormat(Format.TEXT_PLAIN);
    if (!schemas.isEmpty()) {
      String mime = schemas.getFirst().getDefaultVersion().getMimeType();
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
      endPoint.updateReadBytes(packet.available());
      endPoint.getEndPointStatus().incrementReceivedMessages();
      BasePacket basePacket = packetFactory.parseFrame(packet);
      lastAccess.set(System.currentTimeMillis());
      if (basePacket != null) {
        receivedMessage();
        if(basePacket.getType() == TYPE.RST){
          logger.log(COAP_RECEIVED_RESET, packet.getFromAddress());
          close();
          return false;
        }
        logger.log(COAP_PACKET_SENT, basePacket, packet.getFromAddress());
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
    lastAccess.set(System.currentTimeMillis());
    Packet responsePacket = new Packet(mtu, false);
    response.packFrame(responsePacket);
    responsePacket.setFromAddress(response.getFromAddress());
    responsePacket.flip();
    endPoint.sendPacket(responsePacket);
    logger.log(COAP_PACKET_SENT, responsePacket, responsePacket.getFromAddress());
    sentMessage();
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
    return "RFC7252, RFC7641, RFC7959";
  }

  @Override
  public void sendKeepAlive() {
    if (lastAccess.get() < System.currentTimeMillis() + getTimeOut() && subscriptionState.isEmpty()) {
      logger.log(COAP_SESSION_TIMED_OUT, socketAddress, getTimeOut());
      try {
        close();
      } catch (IOException e) {
        // we are closing, we can ignore this
      }
    }
    blockReceiveMonitor.scanForIdle();
  }

  public void ack(BasePacket ackPacket) throws IOException {
    byte[] token = ackPacket.getToken();
    if (token != null) {
      transactionState.ack(ackPacket.getMessageId(), token);
    }
    outboundPipeline.ack(ackPacket);
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    CoapProtocolInformation information = new CoapProtocolInformation();
    updateInformation(information);
    information.setSessionInfo(session.getSessionInformation());
    return information;
  }
}
