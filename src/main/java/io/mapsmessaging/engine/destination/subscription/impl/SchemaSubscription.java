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

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.engine.session.MessageCallback;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.logging.ThreadContext;
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
  public void delete() throws IOException {
    // There is nothing to delete here,
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
