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
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.destination.subscription.impl.ClientSubscribedEventManager;
import io.mapsmessaging.engine.tasks.EngineTask;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.engine.tasks.SubscriptionResponse;

import java.util.concurrent.atomic.AtomicLong;

public class SubscriptionTask extends EngineTask {

  protected final DestinationImpl destination;
  protected final SubscriptionController controller;
  protected final SubscriptionContext context;
  protected final AtomicLong counter;

  public SubscriptionTask(SubscriptionController controller, SubscriptionContext context, DestinationImpl destination, AtomicLong counter) {
    super();
    this.controller = controller;
    this.destination = destination;
    this.context = context;
    this.counter = counter;
  }

  @Override
  public Response taskCall() throws Exception {
    Subscription subscription;
    try {
      if (context.isBrowser() && destination.getResourceType().isQueue()) {
        // We are now looking at the base queue so we need to find "shared_<Name Of Queue>_normal"
        subscription = controller.createBrowserSubscription(context, destination.getSubscription(destination.getFullyQualifiedNamespace()), destination);
      } else {
        subscription = controller.get(destination);
        if (subscription != null) {
          subscription.addContext(context);
        } else {
          subscription = controller.createSubscription(context, destination);
        }
      }
    } finally {
      counter.decrementAndGet();
    }
    return new SubscriptionResponse(new ClientSubscribedEventManager(destination, subscription));
  }
}
