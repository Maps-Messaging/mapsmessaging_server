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

package org.maps.messaging.engine.destination.subscription.tasks;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import org.maps.messaging.engine.destination.DestinationImpl;
import org.maps.messaging.engine.destination.subscription.Subscription;
import org.maps.messaging.engine.destination.subscription.SubscriptionController;
import org.maps.messaging.engine.destination.subscription.set.DestinationSet;
import org.maps.messaging.engine.tasks.BooleanResponse;
import org.maps.messaging.engine.tasks.EngineTask;
import org.maps.messaging.engine.tasks.Response;

public class UnsubscribeTask extends EngineTask {
  private final DestinationImpl destination;
  private final SubscriptionController controller;
  private final DestinationSet destinationSet;
  private final AtomicLong counter;

  public UnsubscribeTask(SubscriptionController controller, DestinationImpl destination, DestinationSet destinationSet, AtomicLong counter){
    super();
    this.controller = controller;
    this.destination = destination;
    this.destinationSet = destinationSet;
    this.counter = counter;
  }

  @Override
  public Response taskCall() throws IOException {
    Subscription subscription;
    try {
      subscription = controller.get(destination);
      if(subscription != null) {
        subscription.removeContext(destinationSet.getContext());
        if (subscription.getContexts().isEmpty()) {
          controller.remove(destination);
          subscription.close();
        }
      }
    } finally {
      counter.decrementAndGet();
    }
    return new BooleanResponse(subscription != null);
  }
}