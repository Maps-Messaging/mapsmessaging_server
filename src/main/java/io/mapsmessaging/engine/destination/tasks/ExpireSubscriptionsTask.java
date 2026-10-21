/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

import io.mapsmessaging.engine.destination.subscription.DestinationSubscriptionManager;
import io.mapsmessaging.engine.tasks.EngineTask;
import io.mapsmessaging.engine.tasks.LongResponse;
import io.mapsmessaging.engine.tasks.Response;

import java.util.Queue;

/** Runs global delivery-state invalidation on the destination subscription queue. */
public final class ExpireSubscriptionsTask extends EngineTask {

  private final DestinationSubscriptionManager subscriptions;
  private final DestinationSubscriptionManager schemaSubscriptions;
  private final Queue<Long> expired;

  public ExpireSubscriptionsTask(DestinationSubscriptionManager subscriptions,
                                 DestinationSubscriptionManager schemaSubscriptions, Queue<Long> expired) {
    this.subscriptions = subscriptions;
    this.schemaSubscriptions = schemaSubscriptions;
    this.expired = expired;
  }

  @Override
  public Response taskCall() {
    int count = 0;
    while (!expired.isEmpty()) {
      long id = expired.remove();
      subscriptions.expired(id);
      schemaSubscriptions.expired(id);
      count++;
    }
    return new LongResponse(count);
  }
}
