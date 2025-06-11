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

package io.mapsmessaging.engine.destination.subscription.modes;

import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.destination.subscription.set.DestinationSet;
import io.mapsmessaging.engine.destination.subscription.tasks.SchemaSubscriptionTask;
import io.mapsmessaging.engine.destination.subscription.tasks.SchemaUnsubscribeTask;
import io.mapsmessaging.engine.destination.subscription.tasks.UnsubscribeTask;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.logging.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class SchemaSubscriptionModeManager  extends SubscriptionModeManager {

  public SchemaSubscriptionModeManager() {
    super(DestinationMode.SCHEMA);
     logger = LoggerFactory.getLogger(SchemaSubscriptionModeManager.class);
  }

  protected Future<Response> scheduleSubscription(SubscriptionController controller, SubscriptionContext context, DestinationImpl destinationImpl, AtomicLong counter) {
     return destinationImpl.submit( new SchemaSubscriptionTask(controller, context, destinationImpl, counter));
  }

  @Override
  protected String adjustUnsubscribeId(String id) {
    return id;
  }

  @Override
  protected UnsubscribeTask createUnsubscribeTask(SubscriptionController controller, DestinationImpl destination, DestinationSet destinationSet, AtomicLong counter) {
    return new SchemaUnsubscribeTask(controller, destination, destinationSet, counter);
  }

}