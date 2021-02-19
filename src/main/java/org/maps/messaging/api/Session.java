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

package org.maps.messaging.api;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maps.messaging.api.features.DestinationType;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.closure.TemporaryDestinationDeletionTask;
import org.maps.messaging.engine.destination.DestinationImpl;
import org.maps.messaging.engine.destination.TemporaryDestination;
import org.maps.messaging.engine.destination.subscription.SubscriptionContext;
import org.maps.messaging.engine.session.MessageCallback;
import org.maps.messaging.engine.session.SecurityContext;
import org.maps.messaging.engine.session.SessionImpl;

public class Session {

  private final SessionImpl sessionImpl;
  private final MessageListener listener;
  private final Map<String, org.maps.messaging.api.Destination> destinations;
  private final Map<String, Transaction> clientTransactions;

  Session(@NonNull @NotNull SessionImpl session, @NonNull @NotNull MessageListener listener) {
    this.sessionImpl = session;
    this.listener = listener;
    clientTransactions = new LinkedHashMap<>();
    session.setMessageCallback(new MessageCallbackImpl());
    destinations = new ConcurrentHashMap<>();
  }

  @NonNull @NotNull SessionImpl getSession() {
    return sessionImpl;
  }

  void close() throws IOException {
    for (Transaction transaction : clientTransactions.values()) {
      transaction.close();
    }
  }

  //<editor-fold desc="Resource Control API">
  public @Nullable org.maps.messaging.api.Destination findTopic(@NonNull @NotNull String destinationName) throws IOException {
    org.maps.messaging.api.Destination result = destinations.get(destinationName);
    if (result == null) {
      DestinationImpl destination = sessionImpl.findDestination(destinationName, DestinationType.TOPIC);
      if(destination != null) {
        if (destination.getResourceType().equals(DestinationType.TOPIC)) {
          result = new Topic(destination);
          destinations.put(result.getName(), result);
        } else {
          throw new IOException("Expected topic but destination is a "+destination.getResourceType().getName());
        }
      }
    }
    return result;
  }

  public @Nullable org.maps.messaging.api.Destination findQueue(@NonNull @NotNull String destinationName) throws IOException {
    org.maps.messaging.api.Destination result = destinations.get(destinationName);
    if (result == null) {
      DestinationImpl destination = sessionImpl.findDestination(destinationName, DestinationType.QUEUE);
      if(destination != null) {
        if (destination.getResourceType().equals(DestinationType.QUEUE)) {
          result = new Queue(destination);
          destinations.put(result.getName(), result);
        } else {
          throw new IOException("Expected a queue but destination is a "+destination.getResourceType().getName());
        }
      }
    }
    return result;
  }

  public DestinationImpl deleteDestination(Destination destination) {
    DestinationImpl deleted = sessionImpl.deleteDestination(destination.destinationImpl);
    if(deleted != null) {
      destinations.remove(deleted.getName());
    }
    return deleted;
  }

  public @Nullable org.maps.messaging.api.Destination findDestination(@NonNull @NotNull String destinationName) throws IOException {
    return findDestination(destinationName, DestinationType.TOPIC);
  }

  public @Nullable org.maps.messaging.api.Destination findDestination(@NonNull @NotNull String destinationName, DestinationType type) throws IOException {
    org.maps.messaging.api.Destination result = destinations.get(destinationName);
    if (result == null) {
      DestinationImpl destination = sessionImpl.findDestination(destinationName, type);
      if(destination != null) {
        if(destination.getResourceType().isTopic()) {
          result = new Topic(destination);
        }
        else{
          result = new Queue(destination);
        }
        if(destination.getResourceType().isTemporary()){
          TemporaryDestinationDeletionTask deletionTask = new TemporaryDestinationDeletionTask((TemporaryDestination) destination);
          sessionImpl.addClosureTask(deletionTask);
        }
        destinations.put(result.getName(), result);
      }
    }
    return result;
  }

  public void deleteResource(@NonNull @NotNull org.maps.messaging.api.Destination destination) {
    sessionImpl.deleteDestination(destination.destinationImpl);
  }
  //</editor-fold>

  public void login() throws IOException {
    sessionImpl.login();
  }

  public @NonNull @NotNull SecurityContext getSecurityContext(){
    return sessionImpl.getSecurityContext();
  }

  public String getName() {
    return sessionImpl.getName();
  }

  public void resumeState() {
    sessionImpl.resumeState();
  }

  public SubscribedEventManager resume(org.maps.messaging.api.Destination destination) {
    return sessionImpl.resume(destination.destinationImpl);
  }

  public void start() {
    sessionImpl.start();
  }

  public SubscribedEventManager addSubscription(@NonNull @NotNull SubscriptionContext context) throws IOException {
    return sessionImpl.addSubscription(context);
  }

  public void hibernateSubscription(@NonNull @NotNull String subscriptionId) {
    sessionImpl.hibernateSubscription(subscriptionId);
  }


  public void removeSubscription(@NonNull @NotNull String subscriptionId) {
    sessionImpl.removeSubscription(subscriptionId);
  }

  public @Nullable WillTask getWillTask() {
    return new WillTask(sessionImpl.getWillTaskImpl());
  }

  public boolean isRestored() {
    return sessionImpl.isRestored();
  }

  public void setExpiryTime(long expiry) {
    sessionImpl.setExpiryTime(expiry);
  }

  public int getReceiveMaximum() {
    return sessionImpl.getReceiveMaximum();
  }

  //<editor-fold desc="Transactional API">
  public @NonNull @NotNull Transaction startTransaction(@NonNull @NotNull String transaction) throws TransactionException {
    if (clientTransactions.containsKey(transaction)) {
      throw new TransactionException("Transaction already exists");
    }
    Transaction clientTransaction = new Transaction(transaction);
    clientTransactions.put(clientTransaction.getTransactionId(), clientTransaction);
    return clientTransaction;
  }

  public @Nullable Transaction getTransaction(@NonNull @NotNull String transaction) {
    return clientTransactions.get(transaction);
  }

  public void closeTransaction(@NonNull @NotNull Transaction transaction) throws IOException {
    transaction.close();
    clientTransactions.remove(transaction.getTransactionId());
  }

  public boolean isClosed() {
    return sessionImpl.isClosed();
  }


  private final class MessageCallbackImpl implements MessageCallback {

    @Override
    public void sendMessage(@NonNull @NotNull DestinationImpl destinationImpl, @NonNull @NotNull SubscribedEventManager subscription, @NonNull @NotNull Message message, @NonNull @NotNull Runnable completionTask) {
      org.maps.messaging.api.Destination destination = destinations.get(destinationImpl.getName());
      if (destination == null) {
        destination = new org.maps.messaging.api.Destination(destinationImpl);
        destinations.put(destination.getName(), destination);
      }
      String normalisedName = sessionImpl.absoluteToNormalised(destination);
      listener.sendMessage(destination, normalisedName, subscription, message, completionTask);
    }
  }
  //</editor-fold>

}
