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

package io.mapsmessaging.network.protocol.impl.stomp.state;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.CloseHandler;
import io.mapsmessaging.network.protocol.impl.stomp.StompProtocol;
import io.mapsmessaging.network.protocol.impl.stomp.StompProtocolException;
import io.mapsmessaging.network.protocol.impl.stomp.frames.CompletionHandler;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Frame;
import lombok.Getter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class SessionState implements CloseHandler, CompletionHandler {

  private final Logger logger;
  private final StompProtocol protocolImpl;
  private final Map<String, SubscribedEventManager> activeSubscriptions;

  private final Map<String, String> destinationMap;

  private int requestCounter;
  private State currentState;
  private boolean isValid;
  @Getter
  private Session session;

  public SessionState(StompProtocol protocolImpl) {
    this.protocolImpl = protocolImpl;
    destinationMap = new ConcurrentHashMap<>();
    logger = protocolImpl.getLogger();
    if (protocolImpl.getEndPoint().isClient()) {
      currentState = new InitialClientState();
    } else {
      currentState = new InitialServerState();
    }
    activeSubscriptions = new LinkedHashMap<>();
    requestCounter = 0;
    session = null;
    isValid = true;
    protocolImpl.getEndPoint().setCloseHandler(this);
  }

  public boolean send(Frame frame) {
    protocolImpl.writeFrame(frame);
    return true;
  }

  public synchronized void handleFrame(Frame frame, boolean endOfBuffer) {
    try {
      protocolImpl.receivedMessage();
      frame.setCallback(this);
      requestCounter++;
      currentState.handleFrame(this, frame, endOfBuffer);
    } catch (IOException e) {
      logger.log(ServerLogMessages.STOMP_FRAME_HANDLE_EXCEPTION, e, frame);
      try {
        protocolImpl.getEndPoint().close();
      } catch (IOException ioException) {
        // Ignore, we have logged the cause and now we are just tidying up
      }
    }
  }

  public synchronized void frameComplete() throws IOException {
    requestCounter--;
    if (requestCounter == 0 && isValid()) {
      protocolImpl.registerRead();
    }
  }

  public void changeState(State newState) {
    currentState = newState;
  }

  public boolean isValid() {
    return isValid;
  }

  public void setSession(Session session) throws StompProtocolException {
    if (session != null) {
      this.session = session;
      protocolImpl.setConnected(true);
      protocolImpl.completedConnection();

    } else {
      throw new StompProtocolException("Session already established");
    }
  }

  public void close() throws IOException {
    isValid = false;
    CompletableFuture<Session> future = SessionManager.getInstance().closeAsync(session, false);
    try {
      future.get();
    } catch (InterruptedException | ExecutionException e) {
      Thread.currentThread().interrupt();
      throw new IOException(e);
    }
  }

  public void shutdown() {
    protocolImpl.close();
  }

  public StompProtocol getProtocol() {
    return protocolImpl;
  }

  public void sendMessage(String normalisedName, SubscriptionContext context, Message message, Runnable completionTask) {
    currentState.sendMessage(this, getMapping(normalisedName), context, message, completionTask);
  }

  public SubscribedEventManager createSubscription(SubscriptionContext context) throws IOException {
    if (context.getFilter().startsWith("/queue") || context.getFilter().startsWith("queue")) {
      getSession().findDestination(context.getFilter(), DestinationType.QUEUE); // See if we have a queue
    }
    SubscribedEventManager subscription = getSession().addSubscription(context);
    activeSubscriptions.put(context.getAlias(), subscription);
    return subscription;
  }

  public SubscribedEventManager findSubscription(String subscriptionId) {
    return activeSubscriptions.get(subscriptionId);
  }

  public void removeSubscription(String subscriptionId) {
    SubscribedEventManager subscription = activeSubscriptions.remove(subscriptionId);
    if (subscription != null) {
      session.removeSubscription(subscriptionId);
    }
  }

  public void run() {
    try {
      frameComplete();
    } catch (IOException e) {
      logger.log(ServerLogMessages.STOMP_STATE_ENGINE_FAILED_COMPLETION);
    }
  }

  public String getMapping(String destinationName) {
    String mappedDestination = destinationMap.get(destinationName);
    if (mappedDestination == null) {
      mappedDestination = destinationName;
    }
    return mappedDestination;
  }

  public Map<String, String> getMap() {
    return destinationMap;
  }

  public void addMapping(String resource, String mappedResource) {
    destinationMap.put(resource, mappedResource);
  }
}
