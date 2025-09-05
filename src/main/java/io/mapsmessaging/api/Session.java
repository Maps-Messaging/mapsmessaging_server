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

package io.mapsmessaging.api;

import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.session.SessionInformationDTO;
import io.mapsmessaging.engine.closure.TemporaryDestinationDeletionTask;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.TemporaryDestination;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.session.MessageCallback;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.engine.session.will.WillDetails;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class Session {

  private static final String SCHEMA = "$schema";

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

  public void login() throws IOException {
    sessionImpl.login();
  }

  public void start() {
    sessionImpl.start();
  }

  @NonNull @NotNull SessionImpl getSession() {
    return sessionImpl;
  }

  void close() throws IOException {
    for (Transaction transaction : clientTransactions.values()) {
      transaction.close();
    }
  }

  public CompletableFuture<Boolean> destinationExists(@NonNull @NotNull String destinationName){
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    future.completeAsync(() -> {
      try {
        return sessionImpl.destinationExists(destinationName).get() != null;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        future.completeExceptionally(e);
      } catch (ExecutionException e) {
        future.completeExceptionally(e);
      }
      return false;
    });
    return future;
  }

  public CompletableFuture<Destination> findDestination(@NonNull @NotNull String destinationName, @NonNull @NotNull DestinationType type) {
    CompletableFuture<Destination> future = new CompletableFuture<>();
    if(destinationName.toLowerCase().startsWith(SCHEMA)){
      destinationName = SCHEMA + destinationName.substring(SCHEMA.length());
    }
    Destination result = destinations.get(destinationName);
    if (result == null) {
      String tmp = destinationName;
      DestinationType tmpMeta = type;
      if (tmp.startsWith(DestinationMode.SCHEMA.getNamespace())) {
        tmp = tmp.substring(DestinationMode.SCHEMA.getNamespace().length());
        tmpMeta = DestinationType.SCHEMA;
      }
      String name = tmp;
      DestinationType meta = tmpMeta;
      Callable<Destination> lookupTask = () -> {
        CompletableFuture<DestinationImpl> destinationCompletableFuture = sessionImpl.findDestination(name, type);
        DestinationImpl destination = destinationCompletableFuture.get();
        Destination end = buildDestination(destination, meta);
        if (end != null) {
          destinations.put(end.getFullyQualifiedNamespace(), end);
        }
        future.complete(end);
        return end;
      };
      future.completeAsync(() -> {
        try {
          return lookupTask.call();
        } catch (Exception e) {
          future.completeExceptionally(e);
          return null;
        }
      });
    } else {
      future.complete(result);
    }
    return future;
  }

  private Destination buildDestination(DestinationImpl destination, DestinationType meta) {
    Destination end = null;
    if (destination != null) {
      switch (meta) {
        case TOPIC:
          end = new Topic(destination);
          break;
        case QUEUE:
          end = new Queue(destination);
          break;
        case SCHEMA:
          end = new Schema(destination);
          break;
        case METRICS:
          end = new Metrics(destination);
          break;
        default:
          break;
      }
      if (destination.getResourceType().isTemporary()) {
        TemporaryDestinationDeletionTask deletionTask = new TemporaryDestinationDeletionTask((TemporaryDestination) destination);
        sessionImpl.addClosureTask(deletionTask);
      }
    }
    return end;
  }

  public CompletableFuture<Void> deleteDestination(Destination destination) {
    return deleteDestinationImpl(destination.destinationImpl);
  }

  public CompletableFuture<Void> deleteDestinationImpl(DestinationImpl destination) {
    CompletableFuture<Void> result = new CompletableFuture<>();
    CompletableFuture<DestinationImpl> future = sessionImpl.deleteDestination(destination);
    future.thenApply(deleted -> {
      if (deleted != null) {
        destinations.remove(deleted.getFullyQualifiedNamespace());
      }
      result.complete(null);
      return deleted;
    });
    return result;
  }

  //</editor-fold>

  public @NonNull @NotNull SecurityContext getSecurityContext() {
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
    if (sessionImpl.getWillTaskImpl() == null) {
      return null;
    }
    return new WillTask(sessionImpl.getWillTaskImpl());
  }

  public void updateWillTopic(WillDetails willDetails) {
    sessionImpl.setWillTask(willDetails);
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
    public void sendMessage(@NonNull @NotNull DestinationImpl destinationImpl, @NonNull @NotNull SubscribedEventManager subscription, @NonNull @NotNull Message message,
        @NonNull @NotNull Runnable completionTask) {
      Destination destination = destinations.get(destinationImpl.getFullyQualifiedNamespace());
      if (destination == null) {
        destination = new Destination(destinationImpl);
        destinations.put(destination.getFullyQualifiedNamespace(), destination);
      }
      String normalisedName = sessionImpl.absoluteToNormalised(destination);
      if (subscription.getContext().getDestinationMode().equals(DestinationMode.SCHEMA)) {
        normalisedName = subscription.getContext().getDestinationMode().getNamespace() + normalisedName;
        normalisedName = normalisedName.replace("//", "/");
      }
      MessageEvent event = new MessageEvent(normalisedName, subscription, message, completionTask);
      listener.sendMessage(event);
    }
  }
  //</editor-fold>

  public SessionInformationDTO getSessionInformation() {
    return sessionImpl.getSessionInformation();
  }

}
