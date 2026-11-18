/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.engine.destination.subscription.modes;


import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.features.RetainHandler;
import io.mapsmessaging.dto.rest.session.SubscriptionStateDTO;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.engine.destination.subscription.set.DestinationSet;
import io.mapsmessaging.engine.destination.subscription.tasks.UnsubscribeTask;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.engine.tasks.ListResponse;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.engine.tasks.SubscriptionResponse;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.ServerLogMessages;
import lombok.Getter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

public abstract class SubscriptionModeManager {

  @Getter
  protected final DestinationMode mode;
  protected final Map<DestinationImpl, Subscription> activeSubscriptions;
  private final Map<DestinationImpl, Map<String, Subscription>> sharedSubscriptions;

  protected Logger logger;

  protected SubscriptionModeManager(DestinationMode mode) {
    this.mode = mode;
    activeSubscriptions = new LinkedHashMap<>();
    sharedSubscriptions = new LinkedHashMap<>();
  }

  public void setDeliveryStateCoordinator(io.mapsmessaging.engine.session.DeliveryStateCoordinator coordinator) {
    for (Subscription subscription : allSubscriptions()) {
      subscription.setDeliveryStateCoordinator(coordinator);
    }
  }

  public java.util.concurrent.CompletableFuture<Void> acknowledgeRecoveredDelivery(
      io.mapsmessaging.storage.delivery.PacketIdStore.Delivery delivery) {
    for (Subscription subscription : allSubscriptions()) {
      if (!subscription.isContextEmpty() && subscription.getDeliveryKey().equals(delivery.topic())) {
        return subscription.acknowledgeRecoveredDelivery(delivery.messageId());
      }
    }
    return null;
  }

  public void shutdown(){
    closeSubscriptions(activeSubscriptions, true);
    closeSharedSubscriptions(true);
  }

  public void close(boolean closeOnly) {
    closeSubscriptions(activeSubscriptions, closeOnly);
    closeSharedSubscriptions(closeOnly);
  }

  public SubscribedEventManager wake(SessionImpl sessionImpl, DestinationImpl destination) {
    for (Subscription subscription : allSubscriptions()) {
      if (subscription.getContext() != null && subscription.getContext().getFilter().equals(destination.getFullyQualifiedNamespace())) {
        subscription.wakeUp(sessionImpl);
        if (subscription instanceof DestinationSubscription destinationSubscription && subscription.getContext() != null && subscription.getContext().getRetainHandler().equals(RetainHandler.SEND_IF_NEW)) {
          queueRetainedMessage((destinationSubscription).getDestinationImpl(), subscription);
        }
      }
    }
    return null;
  }

  public void wakeAll(SessionImpl sessionImpl) {
    for (Subscription subscription : allSubscriptions()) {
      subscription.wakeUp(sessionImpl);
      if (subscription instanceof DestinationSubscription matchedDestinationSubscription && subscription.getContext() != null && subscription.getContext().getRetainHandler().equals(RetainHandler.SEND_IF_NEW)) {
        queueRetainedMessage((matchedDestinationSubscription).getDestinationImpl(), subscription);
      }
    }
  }

  public void hibernateSubscription(String subscriptionId) {
    for (Map.Entry<DestinationImpl, Subscription> entry : activeSubscriptions.entrySet()) {
      if (entry.getKey().getFullyQualifiedNamespace().equals(subscriptionId)) {
        entry.getValue().hibernate();
      }
    }
    for (Map.Entry<DestinationImpl, Map<String, Subscription>> entry : sharedSubscriptions.entrySet()) {
      if (entry.getKey().getFullyQualifiedNamespace().equals(subscriptionId)) {
        entry.getValue().values().forEach(Subscription::hibernate);
      }
    }
  }

  public void hibernate() {
    for (Subscription subscription : allSubscriptions()) {
      subscription.hibernate();
    }
  }

  protected abstract String adjustUnsubscribeId(String id);

  public boolean delSubscription(String id, Map<String, DestinationSet> subscriptions,  SubscriptionController controller) {
    boolean found = false;
    id = adjustUnsubscribeId(id);
    if (id.toLowerCase().startsWith(mode.getNamespace())) {
      DestinationSet destinationSet = subscriptions.remove(id);
      if (destinationSet != null) {
        handleUnsubscribeSet(id, destinationSet, controller);
        found = true;
      }
    }
    return found;
  }

  public void created(SubscriptionController controller, DestinationImpl destinationImpl, Collection<DestinationSet> subscribeList) {
    List<SubscriptionContext> normalList = new ArrayList<>();
    for (DestinationSet subscription : subscribeList) {
      if(subscription.getContext().getDestinationMode().equals(mode) && subscription.add(destinationImpl)) {
        normalList.add(subscription.getContext());
      }
    }

    //
    // OK we have interest and maybe more than one, lucky that the SubscriptionContext is sorted
    // by QoS
    //
    if (!normalList.isEmpty()) {
      List<SubscriptionContext> selected = new ArrayList<>();
      normalList.stream()
          .filter(context -> !context.isSharedSubscription())
          .max(Comparator.comparingInt(context -> context.getQualityOfService().getLevel()))
          .ifPresent(selected::add);
      normalList.stream().filter(SubscriptionContext::isSharedSubscription).forEach(selected::add);
      for (SubscriptionContext context : selected) {
        Future<Response> response = scheduleSubscription(controller, context, destinationImpl, new AtomicLong(1));
        try {
          response.get(60, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          return;
        } catch (ExecutionException | TimeoutException failure) {
          logger.log(ServerLogMessages.SUBSCRIPTION_MGR_CLOSE_SUB_ERROR, failure);
        }
      }
    }
  }

  public void deleted(DestinationImpl destinationImpl, Map<String, DestinationSet> subscriptions) {
    if (activeSubscriptions.containsKey(destinationImpl) || sharedSubscriptions.containsKey(destinationImpl)) {
      //
      // Create a list of destination sets that have this destination in its mapping
      //
      List<DestinationSet> interested = new ArrayList<>();
      subscriptions.values().stream()
          .filter(destinationSet -> destinationSet.contains(destinationImpl))
          .forEachOrdered(interested::add);

      //
      // Now lets see if there is now no interest and clear out any subscription
      //
      if (!interested.isEmpty()) {
        for (DestinationSet destinationSet : interested) {
          clearStructure(destinationImpl, destinationSet, subscriptions);
        }
      }
      activeSubscriptions.remove(destinationImpl);
      sharedSubscriptions.remove(destinationImpl);
    }
  }

  public SubscribedEventManager processSubscriptions(SubscriptionController controller, SubscriptionContext context, DestinationSet destinationSet, boolean isReload) throws IOException {
    AtomicLong counter = new AtomicLong(destinationSet.size());
    SubscribedEventManager subscription = null;
    ListResponse responses = new ListResponse();
    Future<Response> future;
    List<DestinationImpl> interested = destinationSet.stream().toList();
    List<DestinationImpl> diff = interested;
    while(!diff.isEmpty()) {
      for (DestinationImpl destinationImpl : diff) {
        future = scheduleSubscription(controller, context, destinationImpl, counter);
        if (!isReload) {
          responses.addResponse(future);
        }
      }
      List<DestinationImpl> changedList = destinationSet.stream().toList();
      List<DestinationImpl> test = interested;
      diff = changedList.stream()
          .filter(d2 -> test.stream()
              .noneMatch(d1 -> d1 == d2))
          .toList();
      interested = changedList;
    }
    if (!isReload) {
      try {
        List<Response> response = responses.getResponse();
        if (!response.isEmpty()) {
          subscription = ((SubscriptionResponse) response.get(0)).getSubscription();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return null;
      } catch (ExecutionException e) {
        throw new IOException(e);
      }
    }
    return subscription;
  }

  public void add(Subscription subscription, DestinationImpl destinationImpl) {
    SubscriptionContext context = subscription.getContext();
    if (context != null && context.isSharedSubscription()) {
      sharedSubscriptions.computeIfAbsent(destinationImpl, ignored -> new LinkedHashMap<>())
          .put(sharedKey(context), subscription);
    } else {
      activeSubscriptions.put(destinationImpl, subscription);
    }
  }

  public Subscription get(DestinationImpl destination) {
    return activeSubscriptions.get(destination);
  }

  public Subscription get(DestinationImpl destination, SubscriptionContext context) {
    if (!context.isSharedSubscription()) {
      return get(destination);
    }
    Map<String, Subscription> members = sharedSubscriptions.get(destination);
    return members == null ? null : members.get(sharedKey(context));
  }

  public Subscription remove(DestinationImpl destination) {
    return activeSubscriptions.remove(destination);
  }

  public Subscription remove(DestinationImpl destination, SubscriptionContext context) {
    if (!context.isSharedSubscription()) {
      return remove(destination);
    }
    Map<String, Subscription> members = sharedSubscriptions.get(destination);
    if (members == null) {
      return null;
    }
    Subscription removed = members.remove(sharedKey(context));
    if (members.isEmpty()) {
      sharedSubscriptions.remove(destination);
    }
    return removed;
  }

  private String sharedKey(SubscriptionContext context) {
    return context.getKey() + "\u0000" + Objects.toString(context.getSelector(), "");
  }

  private List<Subscription> allSubscriptions() {
    List<Subscription> all = new ArrayList<>(activeSubscriptions.values());
    sharedSubscriptions.values().forEach(group -> all.addAll(group.values()));
    return all;
  }

  private void closeSharedSubscriptions(boolean closeOnly) {
    for (Map<String, Subscription> group : sharedSubscriptions.values()) {
      closeSubscriptions(group, closeOnly);
    }
    sharedSubscriptions.clear();
  }

  protected abstract UnsubscribeTask createUnsubscribeTask( SubscriptionController controller, DestinationImpl destination, DestinationSet destinationSet, AtomicLong counter);

  protected abstract Future<Response> scheduleSubscription(SubscriptionController controller, SubscriptionContext context, DestinationImpl destinationImpl, AtomicLong counter);

  private void handleUnsubscribeSet(String id, DestinationSet destinationSet, SubscriptionController controller) {
    List<DestinationImpl> lostInterest = new ArrayList<>(destinationSet);
    AtomicLong counter = new AtomicLong(lostInterest.size());
    for (DestinationImpl destinationImpl : lostInterest) {
      UnsubscribeTask task;
      if(id.startsWith(mode.getNamespace())){
        task = createUnsubscribeTask(controller, destinationImpl, destinationSet, counter);
        if (destinationImpl.submit(task).isCancelled()) {
          counter.decrementAndGet();
        }
      }
    }
  }

  private void queueRetainedMessage(DestinationImpl destinationImpl, Subscription subscription) {
    if (subscription.getContext() != null && !subscription.getContext().getRetainHandler().equals(RetainHandler.DO_NOT_SEND)) {
      Long retainedId = destinationImpl.getRetainedIdentifier();
      if (retainedId != null) {
        subscription.register(retainedId);
      }
    }
  }

  private void clearStructure(DestinationImpl destinationImpl, DestinationSet destinationSet, Map<String, DestinationSet> subscriptions) {
    if (destinationSet.remove(destinationImpl)
        && destinationSet.isEmpty()
        && !destinationSet.getContext().containsWildcard()) {
      subscriptions.remove(destinationSet.getContext().getKey());
    }
  }

  private void closeSubscriptions(Map<?, Subscription> subscriptions, boolean close){
    List<Subscription> closeList = new ArrayList<>(subscriptions.values());
    subscriptions.clear();
    for (Subscription subscription : closeList) {
      try {
        if (close) {
          subscription.close();
        }
        else{
          subscription.delete();
        }
      } catch (IOException e) {
        logger.log(ServerLogMessages.SUBSCRIPTION_MGR_CLOSE_SUB_ERROR, e);
      }
    }
  }

  public List<SubscriptionStateDTO> getDetails() {
    List<SubscriptionStateDTO> states = new ArrayList<>();
    for(Subscription subscription : activeSubscriptions.values()){
      states.add(subscription.getState());
    }
    return states;
  }
}
