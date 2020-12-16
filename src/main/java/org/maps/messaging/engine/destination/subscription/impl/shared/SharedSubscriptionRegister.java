/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.messaging.engine.destination.subscription.impl.shared;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class manages a unique shared subscription instance. It manages ALL subscriptions under the unique name, this includes
 *
 * a. Simple standard subscriptions
 * b. Selector based subscriptions that would filter a sub set of messages
 *
 * It must also coordinate the allocation of message IDs and the commitment of these IDs across ALL subscriptions to maintain
 * a queue like delivery of once and only once.
 */

public class SharedSubscriptionRegister {

  private final Map<String, SharedSubscriptionManager> activeGroups;


  public SharedSubscriptionRegister(){
    activeGroups = new LinkedHashMap<>();
  }

  public SharedSubscriptionManager get(String name){
    return activeGroups.get(name);
  }

  public SharedSubscriptionManager add(String name, SharedSubscriptionManager sharedSubscriptionManager){
    return activeGroups.put(name, sharedSubscriptionManager);
  }

  public void del(String name){
    activeGroups.remove(name);
  }

}
