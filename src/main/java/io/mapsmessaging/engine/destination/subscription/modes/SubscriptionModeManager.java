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

  protected Logger logger;

  protected SubscriptionModeManager(DestinationMode mode) {
    this.mode = mode;
    activeSubscriptions =  new LinkedHashMap<>();
  }

  public void shutdown(){
    closeSubscriptions(activeSubscriptions, true);
  }

  public void close() {
    closeSubscriptions(activeSubscriptions, false);
  }

  public SubscribedEventManager wake(SessionImpl sessionImpl, DestinationImpl destination) {
    for (Subscription subscription : activeSubscriptions.values()) {
      if (subscription.getContext() != null && subscription.getContext().getFilter().equals(destination.getFullyQualifiedNamespace())) {
        subscription.wakeUp(sessionImpl);
        if (subscription instanceof DestinationSubscription && subscription.getContext() != null && subscription.getContext().getRetainHandler().equals(RetainHandler.SEND_IF_NEW)) {
          queueRetainedMessage(((DestinationSubscription) subscription).getDestinationImpl(), subscription);
        }
      }
    }
    return null;
  }

  public void wakeAll(SessionImpl sessionImpl) {
    for (Subscription subscription : activeSubscriptions.values()) {
      subscription.wakeUp(sessionImpl);
      if (subscription instanceof DestinationSubscription && subscription.getContext() != null && subscription.getContext().getRetainHandler().equals(RetainHandler.SEND_IF_NEW)) {
        queueRetainedMessage(((DestinationSubscription) subscription).getDestinationImpl(), subscription);
      }
    }
  }

  public void hibernateSubscription(String subscriptionId) {
    for (Map.Entry<DestinationImpl, Subscription> entry : activeSubscriptions.entrySet()) {
      if (entry.getKey().getFullyQualifiedNamespace().equals(subscriptionId)) {
        entry.getValue().hibernate();
      }
    }
  }

  public void hibernate() {
    for (Subscription subscription : activeSubscriptions.values()) {
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
      Future<Response> response = scheduleSubscription(controller, normalList.get(0), destinationImpl, new AtomicLong(1));
      try{
        response.get(60, TimeUnit.SECONDS);
      }
      catch (InterruptedException in){
        Thread.currentThread().interrupt();
      } catch( ExecutionException | TimeoutException e) {
        logger.log(ServerLogMessages.SUBSCRIPTION_MGR_CLOSE_SUB_ERROR, e);
      }
    }
  }

  public void deleted(DestinationImpl destinationImpl, Map<String, DestinationSet> subscriptions) {
    if (activeSubscriptions.containsKey(destinationImpl)) {
      //
      // Create a list of destination sets that have this destination in its mapping
      //
      List<DestinationSet> interested = new ArrayList<>();
      for (DestinationSet destinationSet : subscriptions.values()) {
        if (destinationSet.contains(destinationImpl)) {
          interested.add(destinationSet);
        }
      }

      //
      // Now lets see if there is now no interest and clear out any subscription
      //
      if (!interested.isEmpty()) {
        for (DestinationSet destinationSet : interested) {
          clearStructure(destinationImpl, destinationSet, subscriptions);
        }
      }
      activeSubscriptions.remove(destinationImpl);
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
    activeSubscriptions.put(destinationImpl, subscription);
  }

  public Subscription get(DestinationImpl destination) {
    return activeSubscriptions.get(destination);
  }

  public Subscription remove(DestinationImpl destination) {
    return activeSubscriptions.remove(destination);
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
      long retainedId = destinationImpl.getRetainedIdentifier();
      if (retainedId >= 0) {
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

  private void closeSubscriptions(Map<DestinationImpl, Subscription> subscriptions, boolean close){
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
