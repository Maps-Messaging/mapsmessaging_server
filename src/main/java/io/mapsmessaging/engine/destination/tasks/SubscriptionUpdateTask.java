/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.engine.destination.tasks;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.DestinationSubscriptionManager;
import io.mapsmessaging.engine.tasks.LongResponse;
import io.mapsmessaging.engine.tasks.Response;

public class SubscriptionUpdateTask extends SubscriptionTask {

  private final DestinationSubscriptionManager subscriptionManager;
  private final DestinationImpl destination;
  private final Message message;

  public SubscriptionUpdateTask(DestinationImpl destination, DestinationSubscriptionManager subscriptionManager, Message message) {
    super();
    this.destination = destination;
    this.message = message;
    this.subscriptionManager = subscriptionManager;
  }

  @Override
  public Response taskCall() {
    long counter = processSubscription(destination, subscriptionManager, message);
    return new LongResponse(counter);
  }
}