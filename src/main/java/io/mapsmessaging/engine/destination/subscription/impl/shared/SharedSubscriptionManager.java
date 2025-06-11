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

package io.mapsmessaging.engine.destination.subscription.impl.shared;

import io.mapsmessaging.dto.rest.session.SubscriptionStateDTO;
import io.mapsmessaging.engine.destination.subscription.state.BoundedMessageStateManager;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SharedSubscriptionManager {

  @Getter
  private final String name;
  private final Map<String, SharedSubscription> activeSubscriptions;
  private final BoundedMessageStateManager completionHandlerMap;

  public SharedSubscriptionManager(String name) {
    this.name = name;
    activeSubscriptions = new LinkedHashMap<>();
    completionHandlerMap = new BoundedMessageStateManager();
  }

  public void close() {
    for (SharedSubscription sharedSubscription : activeSubscriptions.values()) {
      sharedSubscription.close();
    }
  }

  public BoundedMessageStateManager getMessageStateManager() {
    return completionHandlerMap;
  }

  public SharedSubscription find(String name) {
    return activeSubscriptions.get(name);
  }

  public void add(String name, SharedSubscription subscription) {
    activeSubscriptions.put(name, subscription);
  }

  public SharedSubscription delete(String name) {
    return activeSubscriptions.remove(name);
  }

  public boolean isEmpty() {
    return activeSubscriptions.isEmpty();
  }

  public void complete(long messageIdentifier) {
    for (SharedSubscription sharedSubscription : activeSubscriptions.values()) {
      sharedSubscription.expired(messageIdentifier);
    }
  }

  public List<SubscriptionStateDTO> getSubscriptionStates(){
    List<SubscriptionStateDTO> states = new ArrayList<>();
    for (SharedSubscription sharedSubscription : activeSubscriptions.values()) {
      states.add(sharedSubscription.getState());
    }
    return states;
  }
}
