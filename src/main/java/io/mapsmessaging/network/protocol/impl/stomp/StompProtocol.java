/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.stomp;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.Transformer;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Connect;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Frame;
import io.mapsmessaging.network.protocol.impl.stomp.frames.FrameFactory;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Subscribe;
import io.mapsmessaging.network.protocol.impl.stomp.state.StateEngine;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import static java.nio.channels.SelectionKey.OP_READ;

public class StompProtocol extends ProtocolImpl {

  private final Logger logger;

  private final FrameFactory factory;
  private final StateEngine stateEngine;
  private final SelectorTask selectorTask;
  private Frame activeFrame;
  @Getter
  private String version;

  public StompProtocol(EndPoint endPoint) {
    super(endPoint);
    logger = LoggerFactory.getLogger("STOMP Protocol on " + endPoint.getName());
    logger.log(ServerLogMessages.STOMP_STARTING, endPoint.toString());
    ConfigurationProperties properties = endPoint.getConfig().getProperties();
    int maxBufferSize = DefaultConstants.MAXIMUM_BUFFER_SIZE;
    maxBufferSize = properties.getIntProperty("maximumBufferSize", maxBufferSize);
    version = "1.2";
    selectorTask = new SelectorTask(this, properties);
    factory = new FrameFactory(maxBufferSize, endPoint.isClient());
    activeFrame = null;
    stateEngine = new StateEngine(this);
  }

  public StompProtocol(EndPoint endPoint, Packet packet) throws IOException {
    this(endPoint);
    processPacket(packet);
    selectorTask.getReadTask().pushOutstandingData(packet);
  }

  @Override
  public void close() {
    logger.log(ServerLogMessages.STOMP_CLOSING, endPoint.toString());
    try {
      super.close();
      endPoint.close();
    } catch (IOException e) {
      logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
    }
    selectorTask.close();
  }

  @Override
  public void connect(String sessionId, String username, String password) throws IOException {
    Connect connect = new Connect();
    connect.setLogin(username);
    connect.setPasscode(password);
    connect.setAcceptVersion("1.2");
    writeFrame(connect);
    registerRead();
  }

  @Override
  public void subscribeRemote(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @Nullable Transformer transformer) {
    stateEngine.addMapping(resource, mappedResource);
    if (transformer != null) {
      destinationTransformerMap.put(resource, transformer);
    }

    Subscribe subscribe = new Subscribe();
    subscribe.setDestination(resource);
    subscribe.setId(resource);
    subscribe.setAck("auto");
    writeFrame(subscribe);
  }

  @Override
  public void subscribeLocal(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, String selector, @Nullable Transformer transformer) throws IOException {
    stateEngine.addMapping(resource, mappedResource);
    if (transformer != null) {
      destinationTransformerMap.put(resource, transformer);
    }
    SubscriptionContextBuilder scb = createSubscriptionContextBuilder(resource, selector, QualityOfService.AT_MOST_ONCE, 10240);
    stateEngine.createSubscription(scb.build());
  }

  @Override
  public String getSessionId() {
    if (stateEngine.getSession() == null) {
      return "unknown";
    }
    return stateEngine.getSession().getName();
  }

  public String getName() {
    return "STOMP";
  }

  public void setVersion(float version) {
    this.version = "" + version;
  }

  public void writeFrame(Frame frame) {
    sentMessage();
    selectorTask.push(frame);
    logger.log(ServerLogMessages.STOMP_PUSHED_WRITE, frame);
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    Message message = processTransformer(messageEvent.getDestinationName(), messageEvent.getMessage());
    stateEngine.sendMessage(messageEvent.getDestinationName(), messageEvent.getSubscription().getContext(), message, messageEvent.getCompletionTask());
  }

  // <editor-fold desc="Read Frame functions">
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
      throw eobe; // Do not close on an End Of Buffer Exception
    } catch (IOException e) {
      logger.log(ServerLogMessages.STOMP_PROCESSING_FRAME_EXCEPTION);
      endPoint.close();
      throw e;
    }
    return result;
  }

  @Override
  public void sendKeepAlive() {
    // Nothing to do, yet
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
    if (activeFrame.isValid()) {
      logger.log(ServerLogMessages.RECEIVE_PACKET, activeFrame);
      selectorTask.cancel(OP_READ); // Disable read until this frame is complete
      stateEngine.handleFrame(activeFrame, remaining == 0);
    } else {
      logger.log(ServerLogMessages.STOMP_INVALID_FRAME, frame.toString());
      throw new IOException("Invalid STOMP frame received.. Unable to process" + frame.toString());
    }
    activeFrame = null;
    return remaining != 0;
  }

  public Logger getLogger() {
    return logger;
  }

}
