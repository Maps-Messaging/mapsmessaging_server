/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.stomp.state;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.messaging.api.Destination;
import org.maps.messaging.api.Session;
import org.maps.messaging.api.SessionManager;
import org.maps.messaging.api.SubscribedEventManager;
import org.maps.messaging.api.features.DestinationType;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.destination.subscription.SubscriptionContext;
import org.maps.network.io.CloseHandler;
import org.maps.network.protocol.impl.stomp.StompProtocol;
import org.maps.network.protocol.impl.stomp.StompProtocolException;
import org.maps.network.protocol.impl.stomp.frames.CompletionHandler;
import org.maps.network.protocol.impl.stomp.frames.Frame;

public class StateEngine implements CloseHandler, CompletionHandler {

  private final Logger logger;
  private final StompProtocol protocolImpl;
  private final Map<String, SubscribedEventManager> activeSubscriptions;

  private final Map<String, String> destinationMap;

  private int requestCounter;
  private State currentState;
  private boolean isValid;
  private Session session;

  public StateEngine(StompProtocol protocolImpl) {
    this.protocolImpl = protocolImpl;
    destinationMap = new ConcurrentHashMap<>();
    logger = protocolImpl.getLogger();
    if(protocolImpl.getEndPoint().isClient()){
      currentState = new InitialClientState();
    }
    else{
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
      logger.log(LogMessages.STOMP_FRAME_HANDLE_EXCEPTION, e, frame);
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

  public Session getSession() {
    return session;
  }

  public void setSession(Session session) throws StompProtocolException {
    if (session != null) {
      this.session = session;
      protocolImpl.setConnected(true);
    } else {
      throw new StompProtocolException("Session already established");
    }
  }

  public void close() throws IOException {
    isValid = false;
    SessionManager.getInstance().close(session);
  }

  public void shutdown() {
    protocolImpl.close();
  }

  public StompProtocol getProtocol() {
    return protocolImpl;
  }

  public void sendMessage(Destination destination, String normalisedName, SubscriptionContext context, Message message, Runnable completionTask) {
    currentState.sendMessage(this, destination, normalisedName, context, message, completionTask);
  }

  public SubscribedEventManager createSubscription(SubscriptionContext context) throws IOException {
    if(context.getFilter().startsWith("/queue") || context.getFilter().startsWith("queue")){
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
      logger.log(LogMessages.STOMP_STATE_ENGINE_FAILED_COMPLETION);
    }
  }

  public String map(String destinationName){
    String mappedDestination = destinationName;
    if(destinationMap != null){
      mappedDestination = destinationMap.get(destinationName);
    }
    return mappedDestination;
  }

  public Map<String, String> getMap(){
    return destinationMap;
  }

  public void addMapping(String resource, String mappedResource) {
    destinationMap.put(resource, mappedResource);
  }
}
