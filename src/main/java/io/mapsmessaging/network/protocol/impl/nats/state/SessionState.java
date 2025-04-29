package io.mapsmessaging.network.protocol.impl.nats.state;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.CloseHandler;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocol;
import io.mapsmessaging.network.protocol.impl.nats.frames.*;
import lombok.Getter;
import lombok.Setter;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class SessionState implements CloseHandler, CompletionHandler {

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
  private final NatsProtocol protocol;
  @Getter
  @Setter
  private Session session;
  @Getter
  private final int maxBufferSize;

  private final Logger logger;
  private final Map<String, SubscribedEventManager> activeSubscriptions;
  private final Map<String, String> destinationMap;
  private boolean isValid;
  private long requestCounter;
  private State currentState;

  public SessionState(NatsProtocol protocolImpl) {
    this.protocol = protocolImpl;
    destinationMap = new ConcurrentHashMap<>();
    logger = protocolImpl.getLogger();
    activeSubscriptions = new LinkedHashMap<>();
    session = null;
    isValid = true;
    currentState = new InitialServerState();
    maxBufferSize = protocolImpl.getMaxReceiveSize();
    protocolImpl.getEndPoint().setCloseHandler(this);
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
    PingFrame pingFrame = new PingFrame();
    send(pingFrame);
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

  public SubscribedEventManager createSubscription(SubscriptionContext context) throws IOException {
    if (context.getFilter().startsWith("queue")) {
      getSession().findDestination(context.getFilter(), DestinationType.QUEUE);
    }
    SubscribedEventManager subscription = getSession().addSubscription(context);
    activeSubscriptions.put(context.getAlias(), subscription);
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
}
