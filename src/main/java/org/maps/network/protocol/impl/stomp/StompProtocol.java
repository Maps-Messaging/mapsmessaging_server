/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.network.protocol.impl.stomp;

import static java.nio.channels.SelectionKey.OP_READ;

import java.io.IOException;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.messaging.api.Destination;
import org.maps.messaging.api.SubscribedEventManager;
import org.maps.messaging.api.SubscriptionContextBuilder;
import org.maps.messaging.api.features.ClientAcknowledgement;
import org.maps.messaging.api.message.Message;
import org.maps.network.io.EndPoint;
import org.maps.network.io.Packet;
import org.maps.network.io.impl.SelectorTask;
import org.maps.network.protocol.EndOfBufferException;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.impl.stomp.frames.Connect;
import org.maps.network.protocol.impl.stomp.frames.Frame;
import org.maps.network.protocol.impl.stomp.frames.FrameFactory;
import org.maps.network.protocol.impl.stomp.frames.Subscribe;
import org.maps.network.protocol.impl.stomp.state.StateEngine;
import org.maps.utilities.configuration.ConfigurationProperties;

public class StompProtocol extends ProtocolImpl {

  private final Logger logger;

  private final FrameFactory factory;
  private final StateEngine stateEngine;
  private final SelectorTask selectorTask;
  private Frame activeFrame;
  private String version;

  public StompProtocol(EndPoint endPoint) {
    super(endPoint);
    logger = LoggerFactory.getLogger("STOMP Protocol on " + endPoint.getName());
    logger.log(LogMessages.STOMP_STARTING, endPoint.toString());
    ConfigurationProperties properties = endPoint.getConfig().getProperties();
    int maxBufferSize = DefaultConstants.MAXIMUM_BUFFER_SIZE;
    maxBufferSize = properties.getIntProperty("maximumBufferSize",  maxBufferSize);
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
    logger.log(LogMessages.STOMP_CLOSING, endPoint.toString());
    try {
      super.close();
      endPoint.close();
    } catch (IOException e) {
      logger.log(LogMessages.END_POINT_CLOSE_EXCEPTION, e);
    }
    selectorTask.close();
  }

  @Override
  public void connect() throws IOException {
    Connect connect = new Connect();
    connect.setAcceptVersion("1.2");
    writeFrame(connect);
    registerRead();
  }

  public void subscribeRemote(String resource, String mappedResource) throws IOException{
    stateEngine.addMapping(resource, mappedResource);
    Subscribe subscribe = new Subscribe();
    subscribe.setDestination(resource);
    subscribe.setId(resource);
    subscribe.setAck("auto");
    writeFrame(subscribe);
  }

  public void subscribeLocal(String resource,  String mappedResource) throws IOException {
    stateEngine.addMapping(resource, mappedResource);
    SubscriptionContextBuilder scb = new SubscriptionContextBuilder(resource, ClientAcknowledgement.AUTO);
    scb.setAlias(resource);
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

  public String getVersion() {
    return version;
  }

  public void setVersion(float version) {
    this.version = "" + version;
  }

  public void writeFrame(Frame frame) {
    sentMessage();
    selectorTask.push(frame);
    logger.log(LogMessages.STOMP_PUSHED_WRITE, frame);
  }

  @Override
  public void sendMessage(@NotNull Destination destination, @NotNull String normalisedName, @NotNull SubscribedEventManager subscription, @NotNull Message message, @NotNull Runnable completionTask) {
    stateEngine.sendMessage(destination, normalisedName, subscription.getContext(), message, completionTask);
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
      throw eobe; // Do not close on an End Of Buffer Exception
    } catch (IOException e) {
      e.printStackTrace();
      logger.log(LogMessages.STOMP_PROCESSING_FRAME_EXCEPTION);
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
      logger.log(LogMessages.STOMP_PROCESSING_FRAME, activeFrame);
      selectorTask.cancel(OP_READ); // Disable read until this frame is complete
      stateEngine.handleFrame(activeFrame, remaining == 0);
    } else {
      logger.log(LogMessages.STOMP_INVALID_FRAME, frame.toString());
      throw new IOException("Invalid STOMP frame received.. Unable to process" + frame.toString());
    }

    activeFrame = null;
    return remaining != 0;
  }

  public Logger getLogger() {
    return logger;
  }

}
