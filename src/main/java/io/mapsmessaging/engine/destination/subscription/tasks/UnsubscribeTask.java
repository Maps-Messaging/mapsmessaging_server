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

package io.mapsmessaging.engine.destination.subscription.tasks;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.destination.subscription.set.DestinationSet;
import io.mapsmessaging.engine.tasks.BooleanResponse;
import io.mapsmessaging.engine.tasks.EngineTask;
import io.mapsmessaging.engine.tasks.Response;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class UnsubscribeTask extends EngineTask {

  private final DestinationImpl destination;
  protected final SubscriptionController controller;
  private final DestinationSet destinationSet;
  private final AtomicLong counter;

  public UnsubscribeTask(SubscriptionController controller, DestinationImpl destination, DestinationSet destinationSet, AtomicLong counter) {
    super();
    this.controller = controller;
    this.destination = destination;
    this.destinationSet = destinationSet;
    this.counter = counter;
  }

  protected Subscription lookupSubscription(DestinationImpl destination) {
    return  controller.get(destination);
  }

  protected Subscription removeDestination(DestinationImpl destination) {
    return  controller.remove(destination);
  }

  @Override
  public Response taskCall() throws IOException {
    Subscription subscription;
    try {
      subscription =lookupSubscription(destination);
      if (subscription != null) {
        subscription.removeContext(destinationSet.getContext());
        if (subscription.getContexts().isEmpty()) {
          removeDestination(destination);
          subscription.delete();
        }
      }
    } finally {
      counter.decrementAndGet();
    }
    return new BooleanResponse(subscription != null);
  }
}