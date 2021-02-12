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

package org.maps.messaging.engine.destination.tasks;

import static org.maps.messaging.engine.destination.DestinationImpl.DELETE_PRIORITY;

import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.destination.DestinationImpl;
import org.maps.messaging.engine.destination.subscription.DestinationSubscriptionManager;
import org.maps.messaging.engine.tasks.EngineTask;

public abstract class SubscriptionTask extends EngineTask {

  public long processSubscription(DestinationImpl destination, DestinationSubscriptionManager subscriptionManager, Message message){
    int counter = subscriptionManager.register(message);
    if(counter == 0) {
      if (message.getIdentifier() != destination.getRetainedIdentifier()) {
        RemoveMessageTask remove = new RemoveMessageTask(destination, message.getIdentifier());
        destination.submit(remove, DELETE_PRIORITY );
      }
      destination.getStats().noInterest();
    }
    else {
      destination.getStats().messageSubscribed(counter);
    }
    return counter;
  }

}
