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

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.destination.subscription.impl.SchemaSubscribedEventManager;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.engine.tasks.SubscriptionResponse;
import io.mapsmessaging.schemas.config.SchemaConfig;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

public class SchemaSubscriptionTask extends SubscriptionTask {

  public SchemaSubscriptionTask(SubscriptionController controller,
      SubscriptionContext context, DestinationImpl destination,
      AtomicLong counter) {
    super(controller, context, destination, counter);
  }


  @java.lang.SuppressWarnings("java:S2095")
  @Override
  public Response taskCall() throws Exception {
    Subscription subscription = null;
    try {
      SchemaConfig config = SchemaManager.getInstance().getSchema(destination.getSchema().getUniqueId());
      if (config != null) {
        subscription = controller.getSchema(destination);
        if (subscription != null) {
          subscription.addContext(context);
        } else {
          subscription = controller.createSchemaSubscription(context, destination);
        }
        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.setOpaqueData(config.packAsBytes());
        subscription.sendMessage(messageBuilder.build());
      }
    } finally {
      counter.decrementAndGet();
    }
    return new SubscriptionResponse(new SchemaSubscribedEventManager(subscription));
  }
}
