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

package io.mapsmessaging.network.protocol.impl.nats.state;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.CloseHandler;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocol;
import io.mapsmessaging.network.protocol.impl.nats.frames.*;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.JetStreamRequestManager;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import lombok.Getter;
import lombok.Setter;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class SessionState implements CloseHandler, CompletionHandler {

  @Getter
  private final NatsProtocol protocol;
  @Getter
  private final int maxBufferSize;
  private final Logger logger;
  private final Map<String, SubscribedEventManager> activeSubscriptions;
  private final Map<String, String> destinationMap;
  @Getter
  private final Map<String, List<SubscriptionContext>> subscriptions;
  private final AtomicInteger outstandingPing = new AtomicInteger(0);
  @Getter
  @Setter
  private boolean isVerbose;
  @Getter
  @Setter
  private boolean echoEvents;
  @Getter
  @Setter
  private boolean headers;
  @Getter
  @Setter
  private Session session;
  private boolean isValid;
  private long requestCounter;
  private State currentState;
  @Getter
  private final JetStreamRequestManager jetStreamRequestManager;

  @Getter
  private final Map<String, NamedConsumer> namedConsumers;

  public SessionState(NatsProtocol protocolImpl) {
    this.protocol = protocolImpl;
    destinationMap = new ConcurrentHashMap<>();
    subscriptions = new ConcurrentHashMap<>();
    logger = protocolImpl.getLogger();
    activeSubscriptions = new LinkedHashMap<>();
    session = null;
    isValid = true;
    currentState = new InitialServerState();
    maxBufferSize = protocolImpl.getMaxReceiveSize();
    protocolImpl.getEndPoint().setCloseHandler(this);
    jetStreamRequestManager = new JetStreamRequestManager();
    namedConsumers = new ConcurrentHashMap<>();
  }

  public synchronized void handleFrame(NatsFrame frame, boolean endOfBuffer) {
    try {
      protocol.receivedMessage();
      frame.setCallback(this);
      requestCounter++;
      currentState.handleFrame(this, frame, endOfBuffer);
    } catch (IOException e) {
      logger.log(ServerLogMessages.STOMP_FRAME_HANDLE_EXCEPTION, e, frame);
      try {
        protocol.getEndPoint().close();
      } catch (IOException ioException) {
        // Ignore, we have logged the cause and now we are just tidying up
      }
    }
  }

  private String convertSubject(String subject) {
    return subject
        .replace('.', '/')
        .replace('*', '+')
        .replace('>', '#');
  }

  public boolean contains(String name){
    return namedConsumers.containsKey(name);
  }

  public boolean addNamedConsumer(NamedConsumer namedConsumer) {
    if(!namedConsumers.containsKey(namedConsumer.getName())) {
      namedConsumers.put(namedConsumer.getName(), namedConsumer);
      return true;
    }
    return false;
  }

  public void sendConnect(String username, String password) {
    ConnectFrame connectFrame = new ConnectFrame();
    connectFrame.setCallback(this);
    connectFrame.setUser(username);
    connectFrame.setPass(password);
    send(connectFrame);
  }

  public void sendSubscribe(String subject) {
    SubFrame subFrame = new SubFrame();
    subFrame.setCallback(this);
    subFrame.setSubscriptionId(subject);
    subFrame.setSubject(subject);
    send(subFrame);
  }

  public void sendMessage(String normalisedName, SubscriptionContext context, Message message, Runnable completionTask) {
    currentState.sendMessage(this, getMapping(normalisedName), context, message, completionTask);
  }

  public boolean send(NatsFrame frame) {
    protocol.writeFrame(frame);
    return true;
  }

  public void sendPing() {
    if (outstandingPing.incrementAndGet() > 2) {
      ErrFrame errFrame = new ErrFrame("Ping timed out");
      errFrame.setCompletionHandler(() -> protocol.close());
      send(errFrame);
    } else {
      PingFrame pingFrame = new PingFrame();
      send(pingFrame);
    }

  }

  public void close() throws IOException {
    isValid = false;
    if(session != null) {
      CompletableFuture<Session> future = SessionManager.getInstance().closeAsync(session, false);
      try {
        activeSubscriptions.clear();
        namedConsumers.clear();
        jetStreamRequestManager.close();
        subscriptions.clear();
        future.get();
      } catch (InterruptedException | ExecutionException e) {
        Thread.currentThread().interrupt();
        throw new IOException(e);
      }
    }
  }

  public void shutdown() {
    protocol.close();
  }

  public Subject getSubject() {
    return (session != null) ? session.getSecurityContext().getSubject() : null;
  }

  public void addMapping(String resource, String mappedResource) {
    destinationMap.put(resource, mappedResource);
  }

  public String getMapping(String destinationName) {
    return destinationMap.getOrDefault(destinationName, destinationName);
  }


  public SubscribedEventManager subscribe(String subject, String alias, String shareName, ClientAcknowledgement ackManger, int maxReceive, boolean sync){
    String[] split = subject.split("&");
    String destination = convertSubject(split[0]);
    String selector = split.length > 1 ? split[1] : null;
    SubscriptionContextBuilder builder = new SubscriptionContextBuilder(destination, ackManger);
    builder.setAlias(alias);
    builder.setSync(sync);
    builder.setReceiveMaximum(maxReceive <= 0? protocol.getMaxReceiveSize(): maxReceive);
    builder.setNoLocalMessages(!isEchoEvents());
    if (selector != null) builder.setSelector(selector);
    if (shareName != null) builder.setSharedName(shareName);

    try {
      SubscribedEventManager manager = createSubscription(builder.build());
      if (isVerbose()) send(new OkFrame());
      return manager;
    } catch (IOException ioe) {
      ErrFrame error = new ErrFrame();
      error.setError("Error encounted subscribing to " + destination+", "+ioe.getMessage());
      send(error);
    }
    return null;
  }

  public SubscribedEventManager createSubscription(SubscriptionContext context) throws IOException {
    if (context.getFilter().startsWith("queue")) {
      getSession().findDestination(context.getFilter(), DestinationType.QUEUE);
    }
    List<SubscriptionContext> existing = subscriptions.get(context.getDestinationName());
    if (existing != null) {
      existing.add(context);
      return activeSubscriptions.get(context.getAlias());
    }
    existing = new ArrayList<>();
    existing.add(context);
    subscriptions.put(context.getDestinationName(), existing);
    SubscribedEventManager subscription = getSession().addSubscription(context);
    activeSubscriptions.put(context.getAlias(), subscription);
    session.resumeState();
    return subscription;
  }

  public void removeSubscription(String subscriptionId) {
    SubscribedEventManager subscription = activeSubscriptions.remove(subscriptionId);
    if (subscription != null) {
      session.removeSubscription(subscriptionId);
    }
  }

  public String getSessionId() {
    return (session != null) ? session.getName() : "unknown";
  }

  public Map<String, String> getMap() {
    return destinationMap;
  }

  public void run() {
    try {
      frameComplete();
    } catch (IOException e) {
      logger.log(ServerLogMessages.STOMP_STATE_ENGINE_FAILED_COMPLETION);
    }
  }

  public synchronized void frameComplete() throws IOException {
    requestCounter--;
    if (requestCounter == 0 && isValid) {
      protocol.registerRead();
    }
  }

  public void changeState(State state) {
    currentState = state;
  }

  public void receivedPong() {
    if (outstandingPing.decrementAndGet() < 0) {
      outstandingPing.set(0);
    }
  }

  public PayloadFrame buildPayloadFrame(Message message, String destinationName) {
    PayloadFrame msg;
    if (isHeaders() && !message.getDataMap().isEmpty()) {
      msg = new HMsgFrame(getMaxBufferSize());
      StringBuilder sb = new StringBuilder("NATS/1.0\r\n");
      for (Map.Entry<String, TypedData> entry : message.getDataMap().entrySet()) {
        sb.append(entry.getKey().replace(" ", "_")).append(": ").append("" + entry.getValue().getData()).append("\r\n");
      }
      sb.append("\r\n");
      ((HMsgFrame) msg).setHeaderBytes(sb.toString().getBytes());
    } else {
      msg = new MsgFrame(getMaxBufferSize());
    }
    byte[] payloadData = message.getOpaqueData();
    msg.setSubject(mapMqttTopicToNatsSubject(destinationName));
    if (message.getCorrelationData() != null) {
      msg.setReplyTo(new String(message.getCorrelationData()));
    }
    msg.setPayloadSize(payloadData.length);
    msg.setPayload(payloadData);
    return msg;
  }

  private String mapMqttTopicToNatsSubject(String mqttTopic) {
    return mqttTopic
        .replace('/', '.')
        .replace('+', '*')
        .replace('#', '>');
  }

}
