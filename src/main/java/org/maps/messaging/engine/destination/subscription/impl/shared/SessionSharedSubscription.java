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

package org.maps.messaging.engine.destination.subscription.impl.shared;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.apache.logging.log4j.ThreadContext;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.destination.subscription.OutstandingEventDetails;
import org.maps.messaging.engine.destination.subscription.Subscription;
import org.maps.messaging.engine.destination.subscription.SubscriptionContext;
import org.maps.messaging.engine.destination.subscription.impl.MessageDeliveryCompletionTask;
import org.maps.messaging.engine.destination.subscription.tasks.SharedSubscriptionTask;
import org.maps.messaging.engine.destination.subscription.transaction.AcknowledgementController;
import org.maps.messaging.engine.session.SessionImpl;
import org.maps.network.protocol.ProtocolImpl;

public class SessionSharedSubscription extends Subscription {

  private final SharedSubscription sharedSubscription;
  private final AcknowledgementController acknowledgementController;
  private final MessageDeliveryCompletionTask completionTask;
  private final String sessionId;

  public SessionSharedSubscription(
      SharedSubscription sharedSubscription,
      SessionImpl sessionImpl,
      String sessionId,
      SubscriptionContext context,
      AcknowledgementController acknowledgementController) {
    super(sessionImpl, context);
    this.sessionId = sessionId;
    this.sharedSubscription = sharedSubscription;
    this.acknowledgementController = acknowledgementController;
    completionTask = new MessageDeliveryCompletionTask(sharedSubscription, acknowledgementController);
  }

  @Override
  public void cancel() throws IOException {
    List<OutstandingEventDetails> outstandingEvents = acknowledgementController.getOutstanding();
    for (OutstandingEventDetails outstandingEvent : outstandingEvents) {
      sharedSubscription.handleTransaction(false, outstandingEvent.getId());
    }
    sharedSubscription.removeSession(sessionImpl);
  }

  @Override
  public void close() throws IOException {
    cancel();
  }

  @Override
  public void hibernate() {
    for (OutstandingEventDetails outstanding : acknowledgementController.getOutstanding()) {
      sharedSubscription.getAcknowledgementController().rollback(outstanding.getId());
    }
  }

  @Override
  public boolean expired(long messageIdentifier) {
    // Should be done via the main subscription
    return false;
  }

  public void pause() {
    // Not used
  }

  public void resume() {
    // Not used
  }

  @Override
  public int size() {
    return 1;
  }

  @Override
  public int getPending() {
    return sharedSubscription.getPending();
  }
  public boolean canSend(){
    return acknowledgementController.canSend();
  }

  @Override
  public String getAcknowledgementType() {
    return acknowledgementController.getType();
  }

  @Override
  public Queue<Long> getAllAtRest() {
    return new LinkedList<>();
  }

  @Override
  public Queue<Long> getAll() {
    return new LinkedList<>();
  }

  @Override
  public int register(Message message) {
    // This should be done by the main subscription not here
    return 0;
  }

  @Override
  public int register(long messageId) {
    // This should be done by the main subscription not here
    return 0;
  }

  @Override
  public boolean hasMessage(long messageId) {
    return false; // Should be done by the main subscription, not here
  }

  @Override
  public SessionImpl getSessionImpl() {
    return sessionImpl;
  }

  @Override
  public String getSessionId() {
    return sessionId;
  }

  @Override
  public String getName() {
    return sharedSubscription.getName();
  }

  @Override
  public void rollbackReceived(long messageId) {
    sharedSubscription.getDestinationImpl().submit(new SharedSubscriptionTask(sharedSubscription, acknowledgementController, messageId, false));
  }

  @Override
  public void ackReceived(long messageId) {
    sharedSubscription.getDestinationImpl().submit(new SharedSubscriptionTask(sharedSubscription, acknowledgementController, messageId, true));
  }

  @Override
  public void updateCredit(int credit) {
    acknowledgementController.setMaxOutstanding(credit);
    sharedSubscription.schedule();
  }

  @Override
  public boolean isEmpty() {
    return sharedSubscription.isEmpty();
  }

  @Override
  public int getDepth() {
    return sharedSubscription.getDepth();
  }

  @Override
  public void sendMessage(Message message) {
    String name = "";
    String endpoint = "";
    String version = "";
    ProtocolImpl protocol = sessionImpl.getProtocol();
    if (protocol != null) {
      name = protocol.getName();
      if (protocol.getEndPoint() != null) {
        endpoint = protocol.getEndPoint().getName();
      } else {
        endpoint = "";
      }
      version = protocol.getVersion();
    }
    ThreadContext.put("session", sessionImpl.getName());
    ThreadContext.put("protocol", name);
    ThreadContext.put("endpoint", endpoint);
    ThreadContext.put("version", version);
    acknowledgementController.sent(message);
    if(!sharedSubscription.hasAtRestMessages()){
      message.setLastMessage(true);
    }
    sessionImpl.getMessageCallback().sendMessage(sharedSubscription.getDestinationImpl(), this, message, completionTask);
    ThreadContext.clearMap();
  }

  @Override
  public void run() {
    // no need to run
  }

}
