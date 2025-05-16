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

package io.mapsmessaging.engine.destination.tasks;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.DestinationSubscriptionManager;
import io.mapsmessaging.engine.tasks.EngineTask;

public abstract class SubscriptionTask extends EngineTask {

  public long processSubscription(DestinationImpl destination, DestinationSubscriptionManager subscriptionManager, Message message) {
    int counter = subscriptionManager.register(message);
    if (counter == 0) {
      if (message.getIdentifier() != destination.getRetainedIdentifier()) {
        RemoveMessageTask remove = new RemoveMessageTask(destination, message.getIdentifier());
        destination.submit(remove, DestinationImpl.DELETE_PRIORITY);
      }
      destination.getStats().noInterest();
    } else {
      destination.getStats().messageSubscribed(counter);
    }
    return counter;
  }

}
