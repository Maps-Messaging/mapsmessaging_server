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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class manages a unique shared subscription instance. It manages ALL subscriptions under the unique name, this includes
 *
 * a. Simple standard subscriptions b. Selector based subscriptions that would filter a sub set of messages
 *
 * It must also coordinate the allocation of message IDs and the commitment of these IDs across ALL subscriptions to maintain a queue like delivery of once and only once.
 */

public class SharedSubscriptionRegister {

  private final Map<String, SharedSubscriptionManager> activeGroups;


  public SharedSubscriptionRegister() {
    activeGroups = new LinkedHashMap<>();
  }

  public SharedSubscriptionManager get(String name) {
    return activeGroups.get(name);
  }

  public SharedSubscriptionManager add(String name, SharedSubscriptionManager sharedSubscriptionManager) {
    return activeGroups.put(name, sharedSubscriptionManager);
  }

  public void del(String name) {
    activeGroups.remove(name);
  }

  public List<SubscriptionStateDTO> getState(){

    List<SubscriptionStateDTO> result = new ArrayList<>();
    for(SharedSubscriptionManager manager : activeGroups.values()) {
      result.addAll(manager.getSubscriptionStates());
    }
    return result;
  }

}
