/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.stomp;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.transformers.InterServerTransformation;
import io.mapsmessaging.api.transformers.ParsedMessage;
import io.mapsmessaging.dto.rest.analytics.StatisticsConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.StompConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.StompProtocolInformation;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Connect;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Error;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Frame;
import io.mapsmessaging.network.protocol.impl.stomp.frames.FrameFactory;
import io.mapsmessaging.network.protocol.impl.stomp.frames.HeartBeat;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Subscribe;
import io.mapsmessaging.network.protocol.impl.stomp.state.SessionState;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.filtering.NamespaceFilters;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.nio.channels.SelectionKey.OP_READ;

public class StompProtocol extends Protocol {

  private static final ServerPacket KEEP_ALIVE_FRAME = new KeepAliveFrame();

  @Getter
  private final Logger logger;

  private final FrameFactory factory;
  private final SessionState sessionState;
  private final SelectorTask selectorTask;
  private final int heartbeatCanSendMillis;
  private final int heartbeatWantsReceiveMillis;
  private final int heartbeatToleranceMillis;

  private Frame activeFrame;
  private ScheduledFuture<?> heartbeatFuture;
  private volatile long negotiatedOutgoingHeartbeat;
  private volatile long negotiatedIncomingHeartbeat;

  @Getter
  private final int maxReceiveSize;
  @Getter
  private String version;

  @Getter
  private final boolean base64Encode;

  public StompProtocol(EndPoint endPoint) {
    super(endPoint, endPoint.getConfig().getProtocolConfig("stomp"));
    logger = LoggerFactory.getLogger("STOMP Protocol on " + endPoint.getName());
    logger.log(ServerLogMessages.STOMP_STARTING, endPoint.toString());
    StompConfigDTO stompConfigDTO = (StompConfigDTO) protocolConfig;
    int maxBufferSize = stompConfigDTO.getMaxBufferSize();
    maxReceiveSize = stompConfigDTO.getMaxReceive();
    base64Encode = stompConfigDTO.isBase64EncodeBinary();
    heartbeatCanSendMillis = Math.max(0, stompConfigDTO.getHeartbeatCanSendMillis());
    heartbeatWantsReceiveMillis = Math.max(0, stompConfigDTO.getHeartbeatWantsReceiveMillis());
    heartbeatToleranceMillis = Math.max(0, stompConfigDTO.getHeartbeatToleranceMillis());
    version = "1.2";
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig());
    factory = new FrameFactory(maxBufferSize, endPoint.isClient(), base64Encode);
    activeFrame = null;
    sessionState = new SessionState(this);
  }

  public StompProtocol(EndPoint endPoint, Packet packet) throws IOException {
    this(endPoint);
    processPacket(packet);
    selectorTask.getReadTask().pushOutstandingData(packet);
  }

  @Override
  public void close() {
    logger.log(ServerLogMessages.STOMP_CLOSING, endPoint.toString());
    ScheduledFuture<?> currentHeartbeat = heartbeatFuture;
    heartbeatFuture = null;
    if (currentHeartbeat != null) {
      currentHeartbeat.cancel(false);
    }
    try {
      super.close();
      endPoint.close();
    } catch (IOException e) {
      logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
    }
    selectorTask.close();
  }

  @Override
  public Subject getSubject() {
    if (sessionState.getSession() == null) {
      return new Subject();
    }
    return sessionState.getSession().getSecurityContext().getSubject();
  }

  @Override
  public void setSession(Session session) {
    try {
      sessionState.setSession(session);
    } catch (StompProtocolException e) {
      logger.log(ServerLogMessages.STOMP_FRAME_HANDLE_EXCEPTION, e, session);
    }
  }

  @Override
  public void connect(String sessionId, String username, String password) throws IOException {
    Connect connect = new Connect();
    connect.setLogin(username);
    connect.setPasscode(password);
    connect.setAcceptVersion("1.2");
    connect.setHeartBeat(new HeartBeat(heartbeatCanSendMillis, heartbeatWantsReceiveMillis));
    writeFrame(connect);
    registerRead();
  }

  @Override
  public void subscribeRemote(
      @NonNull @NotNull String resource,
      @NonNull @NotNull String mappedResource,
      @NonNull @NotNull QualityOfService qos,
      @Nullable ParserExecutor executor,
      @Nullable InterServerTransformation transformer,
      StatisticsConfigDTO statistics,
      Map<String, Object> linkProperties) throws IOException {
    super.subscribeRemote(resource, mappedResource, qos, executor, transformer, statistics, linkProperties);
    sessionState.addMapping(resource, mappedResource);
    Subscribe subscribe = new Subscribe();
    subscribe.setDestination(resource);
    subscribe.setId(resource);
    subscribe.setAck(qos.getLevel() > 0 ? "client-individual" : "auto");
    writeFrame(subscribe);
  }

  @Override
  public void subscribeLocal(
      @NonNull @NotNull String resource,
      @NonNull @NotNull String mappedResource,
      @NonNull @NotNull QualityOfService qos,
      String selector,
      @Nullable InterServerTransformation transformer,
      @Nullable NamespaceFilters namespaceFilters,
      StatisticsConfigDTO statistics,
      Map<String, Object> linkProperties) throws IOException {
    super.subscribeLocal(resource, mappedResource, qos, selector, transformer, namespaceFilters, statistics, linkProperties);
    sessionState.addMapping(resource, mappedResource);
    SubscriptionContextBuilder scb = createSubscriptionContextBuilder(resource, selector, qos, 10240);
    sessionState.createSubscription(scb.build());
  }

  @Override
  public String getSessionId() {
    if (sessionState.getSession() == null) {
      return "unknown";
    }
    return sessionState.getSession().getName();
  }

  public String getName() {
    return "STOMP";
  }

  public void setVersion(float version) {
    this.version = Float.toString(version);
  }

  public boolean isStomp12() {
    return "1.2".equals(version);
  }

  public HeartBeat configureHeartBeat(HeartBeat clientHeartBeat) {
    HeartBeat serverHeartBeat =
        new HeartBeat(heartbeatCanSendMillis, heartbeatWantsReceiveMillis);
    negotiatedOutgoingHeartbeat =
        HeartBeat.negotiate(serverHeartBeat.getCanSend(), clientHeartBeat.getWantsReceive());
    negotiatedIncomingHeartbeat =
        HeartBeat.negotiate(clientHeartBeat.getCanSend(), serverHeartBeat.getWantsReceive());
    keepAlive = negotiatedOutgoingHeartbeat;
    startHeartbeatTask();
    return serverHeartBeat;
  }

  public long getNegotiatedOutgoingHeartbeat() {
    return negotiatedOutgoingHeartbeat;
  }

  public long getNegotiatedIncomingHeartbeat() {
    return negotiatedIncomingHeartbeat;
  }

  @Override
  public long getTimeOut() {
    return 0;
  }

  public void writeFrame(Frame frame) {
    sentMessage();
    selectorTask.push(frame);
    logger.log(ServerLogMessages.STOMP_PUSHED_WRITE, frame);
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    ParsedMessage parsedMessage = parseOutboundMessage(messageEvent);
    if (parsedMessage == null) {
      return;
    }
    String topicName = parsedMessage.getDestinationName();
    sessionState.sendMessage(
        topicName,
        messageEvent.getSubscription().getContext(),
        parsedMessage.getMessage(),
        messageEvent.getCompletionTask());
  }

  public void registerRead() throws IOException {
    selectorTask.register(OP_READ);
  }

  public boolean processPacket(Packet packet) throws IOException {
    boolean result = true;
    try {
      while (packet.hasRemaining() && result) {
        result = processEvent(packet);
      }
    } catch (EndOfBufferException eobe) {
      registerRead();
      throw eobe;
    } catch (StompProtocolException protocolError) {
      sendProtocolError(protocolError.getMessage());
      return false;
    } catch (IOException e) {
      logger.log(ServerLogMessages.STOMP_PROCESSING_FRAME_EXCEPTION);
      endPoint.close();
      throw e;
    }
    return result;
  }

  @Override
  public void sendKeepAlive() {
    if (negotiatedOutgoingHeartbeat > 0) {
      selectorTask.push(KEEP_ALIVE_FRAME);
    }
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    StompProtocolInformation information = new StompProtocolInformation();
    updateInformation(information);
    if (sessionState.getSession() != null) {
      information.setSessionInfo(sessionState.getSession().getSessionInformation());
    }
    return information;
  }

  private boolean processEvent(Packet packet) throws IOException {
    Frame frame = activeFrame;
    activeFrame = null;
    try {
      if (!scanFrame(packet, frame)) {
        return false;
      }
    } catch (EndOfBufferException e) {
      registerRead();
      return false;
    }
    return true;
  }

  private boolean scanFrame(Packet packet, Frame frame) throws IOException {
    if (frame == null) {
      frame = factory.parseFrame(packet);
    }
    activeFrame = frame;
    activeFrame.scanFrame(packet);

    int remaining = packet.available();
    if (!activeFrame.isValid()) {
      throw new StompProtocolException("Invalid STOMP frame received: " + frame);
    }
    logger.log(ServerLogMessages.RECEIVE_PACKET, activeFrame);
    selectorTask.cancel(OP_READ);
    sessionState.handleFrame(activeFrame, remaining == 0);
    activeFrame = null;
    return remaining != 0;
  }

  private void sendProtocolError(String message) {
    try {
      selectorTask.cancel(OP_READ);
    } catch (IOException ignored) {
      // The ERROR frame completion closes the connection.
    }
    Error error = new Error();
    error.setContentType("text/plain");
    error.setContent(message.getBytes(StandardCharsets.UTF_8));
    sessionState.send(error);
  }

  private void startHeartbeatTask() {
    ScheduledFuture<?> current = heartbeatFuture;
    if (current != null) {
      current.cancel(false);
    }
    long smallest = smallestPositive(negotiatedOutgoingHeartbeat, negotiatedIncomingHeartbeat);
    if (smallest == 0) {
      heartbeatFuture = null;
      return;
    }
    long checkInterval = Math.max(100, smallest / 2);
    heartbeatFuture =
        SimpleTaskScheduler.getInstance()
            .scheduleAtFixedRate(this::checkHeartbeats, checkInterval, checkInterval, TimeUnit.MILLISECONDS);
  }

  private void checkHeartbeats() {
    long now = System.currentTimeMillis();
    if (negotiatedIncomingHeartbeat > 0
        && now - endPoint.getLastRead()
            > negotiatedIncomingHeartbeat + heartbeatToleranceMillis) {
      close();
      return;
    }
    if (negotiatedOutgoingHeartbeat > 0
        && now - endPoint.getLastWrite() >= negotiatedOutgoingHeartbeat) {
      sendKeepAlive();
    }
  }

  private long smallestPositive(long first, long second) {
    if (first == 0) {
      return second;
    }
    if (second == 0) {
      return first;
    }
    return Math.min(first, second);
  }

  private static final class KeepAliveFrame implements ServerPacket {

    @Override
    public int packFrame(Packet packet) {
      packet.put((byte) '\n');
      return 1;
    }

    @Override
    public void complete() {
      // Nothing to complete for a STOMP heartbeat.
    }

    @Override
    public SocketAddress getFromAddress() {
      return null;
    }
  }
}
