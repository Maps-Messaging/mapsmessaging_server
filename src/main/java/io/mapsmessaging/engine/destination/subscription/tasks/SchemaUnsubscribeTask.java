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

import java.util.concurrent.atomic.AtomicLong;

public class SchemaUnsubscribeTask extends UnsubscribeTask {


  public SchemaUnsubscribeTask(SubscriptionController controller, DestinationImpl destination, DestinationSet destinationSet, AtomicLong counter) {
    super(controller, destination, destinationSet, counter);
  }

  @Override
  protected Subscription lookupSubscription(DestinationImpl destination) {
    return  controller.getSchema(destination);
  }

  @Override
  protected Subscription removeDestination(DestinationImpl destination) {
    return  controller.removeSchema(destination);
  }

}