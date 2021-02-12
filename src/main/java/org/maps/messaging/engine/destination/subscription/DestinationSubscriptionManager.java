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

package org.maps.messaging.engine.destination.subscription;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.messaging.api.message.Message;

public class DestinationSubscriptionManager implements Subscribable{

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

  public Queue<Long> scanForInterest(Queue<Long> removedQueue){
    for (Subscribable subscribable : subscriptions.values()) {
      Queue<Long> interested = subscribable.getAll();
      removedQueue.removeAll(interested);
      if(removedQueue.isEmpty()){
        break; // all events seem to have interest, so nothing to do
      }
    }
    return removedQueue;
  }

  public void put(String name, Subscription subscription) {
    if (!subscriptions.containsKey(name)) {
      logger.log(LogMessages.DESTINATION_SUBSCRIPTION_PUT, subscription.getSessionId(), name, subscription.getName());
      subscriptions.put(name, subscription);
    }
  }

  public Subscribable remove(String name) {
    Subscribable subscribable = subscriptions.remove(name);
    if (subscribable != null) {
      logger.log(LogMessages.SUBSCRIPTION_MGR_REMOVED, name, subscribable.getName());
    }
    return subscribable;
  }

  public void clear() {
    subscriptions.clear();
  }

  @Override
  public int register(Message message) {
    int count =0;
    for (Subscribable subscribable : subscriptions.values()) {
      int updated = subscribable.register(message);
      if(updated != 0){
        count += updated;
      }
    }
    return count;
  }

  @Override
  public boolean hasMessage(long messageIdentifier) {
    boolean response = false;
    for (Subscribable subscribable : subscriptions.values()) {
      response = subscribable.hasMessage(messageIdentifier)|| response;
    }
    return response;
  }

  public boolean expired(long messageIdentifier) {
    boolean response = false;
    for (Subscribable subscribable : subscriptions.values()) {
      response = subscribable.expired(messageIdentifier) || response;
    }
    return response;
  }

  public int size(){
    int size = subscriptions.size();
    for (Subscribable subscribable : subscriptions.values()) {
      size += subscribable.size();
    }
    return size;
  }

  @Override
  public void close() throws IOException {
    for (Subscribable subscribable : subscriptions.values()) {
      subscribable.close();
    }
  }

  @Override
  public String getName() {
    return name;
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
    for(Subscribable subscribable:subscriptions.values()){
      subscribable.pause();
    }
  }

  @Override
  public void resume() {
    for(Subscribable subscribable:subscriptions.values()){
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
}
