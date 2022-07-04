package io.mapsmessaging.engine.destination.subscription.impl;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.session.MessageCallback;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.logging.ThreadContext;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.utilities.threads.tasks.ThreadLocalContext;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class SchemaSubscription extends Subscription {

  protected final DestinationImpl destinationImpl;
  private final SubscribedEventManager eventManager;

  public SchemaSubscription(SessionImpl sessionImpl, DestinationImpl destinationImpl, SubscriptionContext context) {
    super(sessionImpl, context);
    this.destinationImpl = destinationImpl;
    eventManager = new SchemaSubscribedEventManager(this);
    destinationImpl.addSchemaSubscription(this);
  }

  @Override
  public void rollbackReceived(long messageId) {
    // A Schema subscription is a simple deliver a single message when it changes
  }

  @Override
  public void ackReceived(long messageId) {
    // A Schema subscription is a simple deliver a single message when it changes
  }

  @Override
  public void updateCredit(int credit) {
    // A Schema subscription is a simple deliver a single message when it changes
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public int getDepth() {
    return 1;
  }

  @Override
  public int getPending() {
    return 0;
  }

  @Override
  public int register(Message message) {
    sendMessage(message);
    return 1;
  }

  @Override
  public int register(long messageId) {
    return 1;
  }

  @Override
  public boolean hasMessage(long messageIdentifier) {
    return false;
  }

  @Override
  public boolean expired(long messageIdentifier) {
    return false;
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public String getName() {
    return destinationImpl.getFullyQualifiedNamespace();
  }

  @Override
  public Queue<Long> getAll() {
    return new LinkedList<>();
  }

  @Override
  public Queue<Long> getAllAtRest() {
    return new LinkedList<>();
  }

  @Override
  public void pause() {
    // A Schema subscription is a simple deliver a single message when it changes
  }

  @Override
  public void resume() {
    // A Schema subscription is a simple deliver a single message when it changes
  }

  @Override
  public void cancel() throws IOException {
    // A Schema subscription is a simple deliver a single message when it changes
  }

  @Override
  public String getSessionId() {
    return sessionImpl.getName();
  }

  @Override
  public void sendMessage(Message message) {
    ThreadLocalContext.checkDomain(DestinationImpl.SUBSCRIPTION_TASK_KEY);
    SessionImpl session = sessionImpl;
    if (session == null) {
      return;
    }
    ThreadContext.put("session", sessionImpl.getName());
    String name = "";
    String endpoint = "";
    String version = "";
    MessageCallback callback = session.getMessageCallback();
    ProtocolImpl protocol = session.getProtocol();
    if (protocol != null) {
      name = protocol.getName();
      if (protocol.getEndPoint() != null) {
        endpoint = protocol.getEndPoint().getName();
      } else {
        endpoint = "";
      }
      version = protocol.getVersion();
    }
    ThreadContext.put("protocol", name);
    ThreadContext.put("endpoint", endpoint);
    ThreadContext.put("version", version);
    //
    // Update state in an atomic fashion and then send the message
    //
    callback.sendMessage(destinationImpl, eventManager, message, () -> {
      // Nothing to do
    });
    ThreadContext.clear();
  }

  @Override
  public String getAcknowledgementType() {
    return "NoOp";
  }

  @Override
  public void close() throws IOException {
    // A Schema subscription is a simple deliver a single message when it changes

  }

  @Override
  public void run() {
    // A Schema subscription is a simple deliver a single message when it changes

  }
}
