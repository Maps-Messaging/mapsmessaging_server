/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.api;

import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.closure.TemporaryDestinationDeletionTask;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.TemporaryDestination;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.session.MessageCallback;
import io.mapsmessaging.engine.session.SecurityContext;
import io.mapsmessaging.engine.session.SessionImpl;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Session {

  private final SessionImpl sessionImpl;
  private final MessageListener listener;
  private final Map<String, Destination> destinations;
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

  public DestinationImpl deleteDestination(Destination destination) {
    DestinationImpl deleted = sessionImpl.deleteDestination(destination.destinationImpl);
    if(deleted != null) {
      destinations.remove(deleted.getFullyQualifiedNamespace());
    }
    return deleted;
  }

  public @Nullable Destination findDestination(@NonNull @NotNull String destinationName, DestinationType type) throws IOException {
    Destination result = destinations.get(destinationName);
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
        destinations.put(result.getFullyQualifiedNamespace(), result);
      }
    }
    return result;
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

  public SubscribedEventManager resume(Destination destination) {
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
      Destination destination = destinations.get(destinationImpl.getFullyQualifiedNamespace());
      if (destination == null) {
        destination = new Destination(destinationImpl);
        destinations.put(destination.getFullyQualifiedNamespace(), destination);
      }
      String normalisedName = sessionImpl.absoluteToNormalised(destination);
      MessageEvent event = new MessageEvent(normalisedName, subscription, message, completionTask);
      listener.sendMessage(event);
    }
  }
  //</editor-fold>

}
