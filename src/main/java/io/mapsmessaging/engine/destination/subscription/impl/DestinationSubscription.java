/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.engine.destination.subscription.impl;

import io.mapsmessaging.admin.SubscriptionJMX;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.Subscribable;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManager;
import io.mapsmessaging.engine.destination.subscription.transaction.AcknowledgementController;
import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.engine.session.MessageCallback;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.logging.ThreadContext;
import io.mapsmessaging.utilities.threads.tasks.ThreadLocalContext;
import lombok.Getter;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.util.Map;
import java.util.Queue;

/**
 * Note: This is a complex class that maintains the state of events for a specific subscription to a specific destination.
 */
public class DestinationSubscription extends Subscription {

  @Getter
  private final AcknowledgementController acknowledgementController;
  private final MessageDeliveryCompletionTask completionTask;

  protected final Logger logger;
  private final SubscriptionJMX mbean;
  private final String sessionId;

  protected final DestinationImpl destinationImpl;
  protected final MessageStateManager messageStateManager;

  protected DestinationSubscription activeSubscription;
  protected ClientSubscribedEventManager eventStateManager;
  private boolean isPaused;

  protected long messagesIgnored;
  protected long messagesRegistered;
  protected long messagesSent;
  protected long messagesAcked;
  protected long messagesRolledBack;
  protected long messagesExpired;

  // <editor-fold desc="Life cycle functions">
  public DestinationSubscription(DestinationImpl destinationImpl,
      SubscriptionContext context,
      SessionImpl sessionImpl,
      String sessionId,
      AcknowledgementController acknowledgementController,
      MessageStateManager messageStateManager) {
    super(sessionImpl, context);
    logger = LoggerFactory.getLogger(DestinationSubscription.class);
    this.eventStateManager = new ClientSubscribedEventManager(destinationImpl, this);
    this.destinationImpl = destinationImpl;
    this.messageStateManager = messageStateManager;
    this.acknowledgementController = acknowledgementController;
    this.sessionId = sessionId;
    activeSubscription = this;
    messagesSent = 0;
    messagesRegistered = 0;
    messagesIgnored = 0;
    messagesExpired = 0;
    isPaused = false;
    mbean = new SubscriptionJMX(destinationImpl.getTypePath(), this);
    completionTask = new MessageDeliveryCompletionTask(this, acknowledgementController);
    destinationImpl.addSubscription(this);
  }

  @Override
  public void close() {
    mbean.close();
    acknowledgementController.close();
    messageStateManager.rollbackInFlightMessages();

    // We need to see if any other subscriptions have interest in these events, and if not then simply
    // remove the events. Just like when we deliver all the events in a subscription
    try {
      messageStateManager.close();
    } catch (IOException e) {
      logger.log(ServerLogMessages.DESTINATION_SUBSCRIPTION_EXCEPTION_ON_CLOSE, e);
    }
  }

  public void delete() {
    mbean.close();
    acknowledgementController.close();
    messageStateManager.rollbackInFlightMessages();
    destinationImpl.removeSubscription(sessionId);

    // We need to see if any other subscriptions have interest in these events, and if not then simply
    // remove the events. Just like when we deliver all the events in a subscription
    try {
      messageStateManager.delete();
    } catch (IOException e) {
      logger.log(ServerLogMessages.DESTINATION_SUBSCRIPTION_EXCEPTION_ON_CLOSE, e);
    }
  }


  @Override
  public void hibernate() {
    logger.log(ServerLogMessages.DESTINATION_SUBSCRIPTION_HIBERNATE, destinationImpl.getFullyQualifiedNamespace(), sessionId);
    acknowledgementController.clear();
    messageStateManager.rollbackInFlightMessages();
    //
    // Remove the session since it is now redundant
    //
    super.hibernate();
  }

  @Override
  public void wakeUp(SessionImpl sessionImpl) {
    if (this.sessionImpl == null && hibernating && sessionImpl != null) {
      super.wakeUp(sessionImpl);
      logger.log(ServerLogMessages.DESTINATION_SUBSCRIPTION_WAKEUP, destinationImpl.getFullyQualifiedNamespace(), sessionImpl.getName());
      schedule();
    }
  }

  @Override
  public void cancel() throws IOException {
    Subscribable sub = destinationImpl.removeSubscription(sessionId);
    if (sub != null) {
      sub.close();
    }
  }
  // </editor-fold>

  // <editor-fold desc="Get Functions">


  @Override
  public int size() {
    return 1;
  }

  public MessageDeliveryCompletionTask getCompletionTask() {
    return completionTask;
  }

  @Override
  public String getSessionId() {
    return sessionId;
  }

  @Override
  public String getName() {
    return destinationImpl.getFullyQualifiedNamespace();
  }

  public long getMessagesSent() {
    return messagesSent;
  }

  public long getMessagesAcked() {
    return messagesAcked;
  }

  public long getMessagesRolledBack() {
    return messagesRolledBack;
  }

  public int getInFlight() {
    return acknowledgementController.size();
  }

  @Override
  public String getAcknowledgementType() {
    return acknowledgementController.getType();
  }

  @Override
  public Queue<Long> getAllAtRest() {
    return messageStateManager.getAllAtRest();
  }

  @Override
  public Queue<Long> getAll() {
    return messageStateManager.getAll();
  }

  public DestinationImpl getDestinationImpl() {
    return destinationImpl;
  }

  public boolean hasAtRestMessages() {
    return messageStateManager.hasAtRestMessages();
  }

  @Override
  public int getDepth() {
    return messageStateManager.size();
  }

  @Override
  public int getPending() {
    return messageStateManager.pending();
  }

  // </editor-fold>

  @Override
  public boolean hasMessage(long messageId) {
    ThreadLocalContext.checkDomain(DestinationImpl.SUBSCRIPTION_TASK_KEY);

    return messageStateManager.hasMessage(messageId);
  }

  // <editor-fold desc="Transactional functions">
  @Override
  public void ackReceived(long messageId) {
    handleTransaction(true, messageId);
  }

  @Override
  public void updateCredit(int credit) {
    if (acknowledgementController.setMaxOutstanding(credit)) {
      destinationImpl.scanForDelivery(this);
    }
  }

  @Override
  public boolean isEmpty() {
    return !(messageStateManager.hasAtRestMessages() || messageStateManager.hasMessagesInFlight());
  }

  @Override
  public void rollbackReceived(long messageId) {
    handleTransaction(false, messageId);
  }

  public MessageStateManager getMessageStateManager() {
    return messageStateManager;
  }

  @Override
  public void sendMessage(Message message) {
    ThreadLocalContext.checkDomain(DestinationImpl.SUBSCRIPTION_TASK_KEY);
    SessionImpl session = sessionImpl;
    if (session == null) {
      return;
    }
    ThreadContext.put("session", sessionId);
    String name = "";
    String endpoint = "";
    String version = "";
    MessageCallback callback = session.getMessageCallback();
    ClientConnection clientConnection = session.getClientConnection();
    if (clientConnection != null) {
      name = clientConnection.getName();
      endpoint = clientConnection.getUniqueName();
      version = clientConnection.getVersion();
    }
    ThreadContext.put("protocol", name);
    ThreadContext.put("endpoint", endpoint);
    ThreadContext.put("version", version);
    //
    // Update state in an atomic fashion and then send the message
    //
    if (!hasAtRestMessages()) {
      message.setLastMessage(true);
    }
    acknowledgementController.sent(message);
    eventStateManager.setSubscription(activeSubscription);
    callback.sendMessage(destinationImpl, eventStateManager, message, completionTask);
    logger.log(ServerLogMessages.DESTINATION_SUBSCRIPTION_SEND, destinationImpl.getFullyQualifiedNamespace(), sessionId, message.getIdentifier());
    ThreadContext.clear();
  }

  @Override
  public boolean expired(long messageIdentifier) {
    ThreadLocalContext.checkDomain(DestinationImpl.SUBSCRIPTION_TASK_KEY);
    messageStateManager.expired(messageIdentifier);
    return true;
  }

  @Override
  public int register(Message message) {
    ThreadLocalContext.checkDomain(DestinationImpl.SUBSCRIPTION_TASK_KEY);
    Map<String, String> metaData = message.getMeta();
    if (metaData != null) {
      String messageSession = metaData.get("sessionId");
      SubscriptionContext context = getContext();
      if (context != null
          && messageSession != null
          && context.noLocalMessages()
          && messageSession.equals(sessionId)) {
        messagesIgnored++;
        return 0;
      }
    }
    if (!hibernating || message.isStoreOffline()) {
      messageStateManager.register(message);
      messagesRegistered++;
      schedule();
      return 1;
    }
    messagesIgnored++;
    return 0;
  }

  @Override
  public int register(long messageId) {
    ThreadLocalContext.checkDomain(DestinationImpl.SUBSCRIPTION_TASK_KEY);
    if (!hibernating) {
      messageStateManager.register(messageId);
      schedule();
      return 1;
    }
    return 0;
  }

  public void handleTransaction(boolean isAck, long messageId) {
    ThreadLocalContext.checkDomain(DestinationImpl.SUBSCRIPTION_TASK_KEY);

    if (isAck) {
      logger.log(ServerLogMessages.DESTINATION_SUBSCRIPTION_ACK, messageId, destinationImpl.getFullyQualifiedNamespace(), sessionId);
      acknowledgementController.ack(messageId);
      messageStateManager.commit(messageId);
      messagesAcked++;
      destinationImpl.complete(messageId);
    } else {
      logger.log(ServerLogMessages.DESTINATION_SUBSCRIPTION_ROLLBACK, messageId, destinationImpl.getFullyQualifiedNamespace(), sessionId);
      acknowledgementController.rollback(messageId);
      messageStateManager.rollback(messageId);
      messagesRolledBack++;
    }
    schedule();
  }

  // </editor-fold>

  // <editor-fold desc="Function to write data to the client">

  public void acknowledgePreviousEvent() {
    ThreadLocalContext.checkDomain(DestinationImpl.SUBSCRIPTION_TASK_KEY);

    //
    // Chained completion handlers
    //
    long messageId = acknowledgementController.messageSent();
    if (messageId > -1) {
      completeMessage(messageId);
    }
    schedule();
  }

  public void completeMessage(long messageId) {
    messageStateManager.commit(messageId);
    messagesAcked++;
    destinationImpl.complete(messageId);
  }

  @Override
  public void run() {
    ThreadLocalContext.checkDomain(DestinationImpl.SUBSCRIPTION_TASK_KEY);
    try {
      //
      // Now check to see if we can send any more events
      //
      while (isReady()) {
        long nextMessageId = messageStateManager.nextMessageId();
        if (nextMessageId >= 0) {
          Message message = destinationImpl.getMessage(nextMessageId);
          if (message != null) {
            messageStateManager.allocate(message);
            sendMessage(message);
            messagesSent++;
          } else {
            messageStateManager.expired(nextMessageId);
            messagesExpired++;
          }
        } else {
          break;
        }
      }
    } catch (IOException e) {
      logger.log(ServerLogMessages.DESTINATION_SUBSCRIPTION_TASK_FAILURE, e, destinationImpl.getFullyQualifiedNamespace(), sessionImpl.getName());
    } catch (CancelledKeyException ignore) {
      // We get these because the End Point could be closed
    } catch (RuntimeException th) {
      logger.log(ServerLogMessages.DESTINATION_SUBSCRIPTION_TASK_FAILURE, destinationImpl.getFullyQualifiedNamespace(), sessionImpl.getName(), th);
    }
  }

  //
  // So we have a message to send, but we are not scheduled to run
  //
  public boolean schedule() {
    if (isReady()) {
      destinationImpl.scanForDelivery(this);
      return true;
    }
    return false;
  }

  protected boolean isReady() {
    return (!isPaused && !hibernating && messageStateManager.hasAtRestMessages() && !getContexts().isEmpty() && acknowledgementController.canSend());
  }

  // </editor-fold>

  // <editor-fold desc="Subscription pause/resume functions">
  public boolean isPaused() {
    return isPaused;
  }

  @Override
  public void pause() {
    isPaused = true;
  }

  @Override
  public void resume() {
    isPaused = false;
    schedule();
  }
  // </editor-fold>

  @Override
  public String toString() {
    return "SessionID:" + sessionId
        + " Ack Controller:" + acknowledgementController.toString()
        + " Destination:" + destinationImpl.getFullyQualifiedNamespace()
        + " MessageState:" + messageStateManager.toString()
        + " Paused:" + isPaused
        + " Sent:" + messagesSent
        + " Acked:" + messagesAcked
        + " Rolled:" + messagesRolledBack
        + " Hibernating:" + hibernating
        + super.toString();
  }
}
