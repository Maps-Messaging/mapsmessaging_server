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

package io.mapsmessaging.engine.destination.subscription;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.session.SubscriptionStateDTO;
import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.collections.NaturalOrderedLongQueue;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;

import java.io.IOException;
import java.util.*;

public class DestinationSubscriptionManager implements Subscribable {

  private final Logger logger;
  private final Map<String, Subscribable> subscriptions;
  private final String name;

  public DestinationSubscriptionManager(String name) {
    logger = LoggerFactory.getLogger(DestinationSubscriptionManager.class);
    subscriptions = new LinkedHashMap<>();
    this.name = name;
  }

  public boolean hasSubscriptions() {
    return !subscriptions.isEmpty();
  }

  public boolean hasInterest(long messageId) {
    for (Subscribable subscribable : subscriptions.values()) {
      if (subscribable.hasMessage(messageId)) {
        return true;
      }
    }
    return false;
  }

  public Queue<Long> scanForInterest(Queue<Long> removedQueue) {
    for (Subscribable subscribable : subscriptions.values()) {
      Queue<Long> interested = subscribable.getAll();
      removedQueue.removeAll(interested);
      if (removedQueue.isEmpty()) {
        break; // all events seem to have interest, so nothing to do
      }
    }
    return removedQueue;
  }

  public void put(String name, Subscription subscription) {
    subscriptions.computeIfAbsent(name, k -> {
      logger.log(ServerLogMessages.DESTINATION_SUBSCRIPTION_PUT, subscription.getSessionId(), name, subscription.getName());
      return subscription;
    });
  }

  public Subscribable remove(String name) {

    Subscribable subscribable = subscriptions.remove(name);
    if (subscribable != null) {
      logger.log(ServerLogMessages.SUBSCRIPTION_MGR_REMOVED, name, subscribable.getName());
    }
    return subscribable;
  }

  public void clear() {
    subscriptions.clear();
  }

  @Override
  public int register(Message message) {
    int count = 0;
    for (Subscribable subscribable : subscriptions.values()) {
      int updated = subscribable.register(message);
      if (updated != 0) {
        count += updated;
      }
    }
    return count;
  }

  @Override
  public boolean hasMessage(long messageIdentifier) {
    for (Subscribable subscribable : subscriptions.values()) {
      if (subscribable.hasMessage(messageIdentifier)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean expired(long messageIdentifier) {
    for (Subscribable subscribable : subscriptions.values()) {
      if (subscribable.expired(messageIdentifier)) {
        return true;
      }
    }
    return false;
  }


  @Override
  public int size() {
    int size = subscriptions.size();
    for (Subscribable subscribable : subscriptions.values()) {
      size += subscribable.size();
    }
    return size;
  }

  @Override
  public void close() throws IOException {
    List<Subscribable> closeList = new ArrayList<>(subscriptions.values());
    for (Subscribable subscribable : closeList) {
      subscribable.close();
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Queue<Long> getAll() {
    Queue<Long> keepList = new NaturalOrderedLongQueue(0, new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
    for (Subscribable subscribable : subscriptions.values()) {
      Queue<Long> interested = subscribable.getAll();
      keepList.addAll(interested);
    }
    return keepList;
  }

  @Override
  public Queue<Long> getAllAtRest() {
    return new ArrayDeque<>();
  }

  @Override
  public void pause() {
    for (Subscribable subscribable : subscriptions.values()) {
      subscribable.pause();
    }
  }

  @Override
  public void resume() {
    for (Subscribable subscribable : subscriptions.values()) {
      subscribable.resume();
    }
  }

  @Override
  public int register(long messageId) {
    return 0;
  }

  public Subscribable getSubscription(String subscriptionName) {
    return subscriptions.get(subscriptionName);
  }

  public List<SubscriptionStateDTO> getSubscriptionStates() {
    List<SubscriptionStateDTO> states = new ArrayList<>();
    for (Subscribable subscribable : subscriptions.values()) {
      if (subscribable instanceof Subscription){
        states.add( ((Subscription)subscribable).getState());
      }
    }
    return states;
  }
}
