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

package io.mapsmessaging.network.protocol.impl.nats;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.Transformer;
import io.mapsmessaging.dto.rest.config.protocol.impl.NatsConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.NatsProtocolInformation;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.nats.frames.FrameFactory;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.filtering.NamespaceFilters;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import java.io.IOException;

import static java.nio.channels.SelectionKey.OP_READ;

public class NatsProtocol extends Protocol {

  @Getter
  private final Logger logger;

  private final SelectorTask selectorTask;
  @Getter
  private final FrameFactory factory;
  @Getter
  private final int maxReceiveSize;
  private final SessionState sessionState;
  @Getter
  private final NatsConfigDTO natsConfig;
  private NatsFrame activeFrame;

  public NatsProtocol(EndPoint endPoint) {
    super(endPoint, endPoint.getConfig().getProtocolConfig("nats"));
    logger = LoggerFactory.getLogger("NATS Protocol on " + endPoint.getName());
    logger.log(ServerLogMessages.NATS_STARTING, endPoint.toString());
    natsConfig = (NatsConfigDTO) protocolConfig;
    int maxBufferSize = natsConfig.getMaxBufferSize();
    maxReceiveSize = natsConfig.getMaxReceive();
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig());
    factory = new FrameFactory(maxBufferSize, false);
    sessionState = new SessionState(this);
    keepAlive = natsConfig.getKeepAlive();
  }

  public NatsProtocol(EndPoint endPoint, Packet packet) throws IOException {
    this(endPoint);
    if (packet != null) {
      processPacket(packet);
      selectorTask.getReadTask().pushOutstandingData(packet);
    } else {
      registerRead();
    }
  }

  @Override
  public void close() {
    logger.log(ServerLogMessages.NATS_CLOSING, endPoint.toString());
    try {
      super.close();
      sessionState.close();
    } catch (IOException e) {
      logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
    }
    selectorTask.close();
  }

  @Override
  public Subject getSubject() {
    return sessionState.getSubject();
  }

  @Override
  public void connect(String sessionId, String username, String password) throws IOException {
    sessionState.sendConnect(username, password);
    registerRead();
  }

  @Override
  public void subscribeRemote(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @Nullable ParserExecutor executor, @Nullable Transformer transformer) {
    sessionState.addMapping(resource, mappedResource);
    if (transformer != null) {
      destinationTransformerMap.put(resource, transformer);
    }
    sessionState.sendSubscribe(resource);
  }

  @Override
  public void subscribeLocal(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, String selector, @Nullable Transformer transformer, @Nullable NamespaceFilters namespaceFilters) throws IOException {
    sessionState.addMapping(resource, mappedResource);
    if (transformer != null) {
      destinationTransformerMap.put(resource, transformer);
    }
    SubscriptionContextBuilder scb = createSubscriptionContextBuilder(resource, selector, QualityOfService.AT_MOST_ONCE, 10240);
    sessionState.createSubscription(scb.build());
  }

  @Override
  public String getSessionId() {
    return sessionState.getSessionId();
  }

  @Override
  public String getVersion() {
    return "2.0";
  }

  public String getName() {
    return "NATS";
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    Message message = processTransformer(messageEvent.getDestinationName(), messageEvent.getMessage());
    sessionState.sendMessage(messageEvent.getDestinationName(), messageEvent.getSubscription().getContext(), message, messageEvent.getCompletionTask());
  }

  @Override
  public void sendKeepAlive() {
    sessionState.sendPing();
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    NatsProtocolInformation information = new NatsProtocolInformation();
    updateInformation(information);
    information.setSessionInfo(sessionState.getSession().getSessionInformation());
    return information;
  }

  public void registerRead() throws IOException {
    selectorTask.register(OP_READ);
  }

  public void writeFrame(NatsFrame frame) {
    sentMessage();
    selectorTask.push(frame);
    logger.log(ServerLogMessages.STOMP_PUSHED_WRITE, frame);
  }

  public boolean processPacket(Packet packet) throws IOException {
    boolean result = true;
    try {
      while (packet.hasRemaining() && result) {
        result = processEvent(packet);
      }
    } catch (EndOfBufferException eobe) {
      registerRead();
      throw eobe; // Do not close on an End Of Buffer Exception
    } catch (IOException e) {
      logger.log(ServerLogMessages.STOMP_PROCESSING_FRAME_EXCEPTION);
      endPoint.close();
      throw e;
    }
    return result;
  }

  private boolean processEvent(Packet packet) throws IOException {
    NatsFrame frame = activeFrame;
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

  private boolean scanFrame(Packet packet, NatsFrame frame) throws IOException {
    if (frame == null) {
      frame = factory.parseFrame(packet);
    }
    activeFrame = frame;
    activeFrame.parseFrame(packet);

    int remaining = packet.available();
    if (activeFrame.isValid()) {
      logger.log(ServerLogMessages.RECEIVE_PACKET, activeFrame);
      selectorTask.cancel(OP_READ); // Disable read until this frame is complete
      sessionState.handleFrame(activeFrame, remaining == 0);
    } else {
      logger.log(ServerLogMessages.STOMP_INVALID_FRAME, frame.toString());
      throw new IOException("Invalid STOMP frame received.. Unable to process" + frame);
    }
    activeFrame = null;
    return remaining != 0;
  }
}
