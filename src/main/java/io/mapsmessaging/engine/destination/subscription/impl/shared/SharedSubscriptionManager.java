/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

package io.mapsmessaging.engine.destination.subscription.impl.shared;

import io.mapsmessaging.engine.destination.subscription.state.BoundedMessageStateManager;
import java.util.LinkedHashMap;
import java.util.Map;

public class SharedSubscriptionManager {

  private final String name;
  private final Map<String, SharedSubscription> activeSubscriptions;
  private final BoundedMessageStateManager completionHandlerMap;

  public SharedSubscriptionManager(String name) {
    this.name = name;
    activeSubscriptions = new LinkedHashMap<>();
    completionHandlerMap = new BoundedMessageStateManager();
  }

  public String getName(){
    return name;
  }

  public void close() {
    for (SharedSubscription sharedSubscription : activeSubscriptions.values()) {
      sharedSubscription.close();
    }
  }

  public BoundedMessageStateManager getMessageStateManager(){
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

  public boolean isEmpty(){
    return activeSubscriptions.isEmpty();
  }

  public void complete(long messageIdentifier){
    for(SharedSubscription sharedSubscription:activeSubscriptions.values()){
      sharedSubscription.expired(messageIdentifier);
    }
  }
}
