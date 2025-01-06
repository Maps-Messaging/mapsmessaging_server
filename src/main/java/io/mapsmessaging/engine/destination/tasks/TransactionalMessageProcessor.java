/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
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
import io.mapsmessaging.engine.destination.delayed.MessageManager;
import io.mapsmessaging.engine.destination.subscription.DestinationSubscriptionManager;

public class TransactionalMessageProcessor extends MessageProcessor {

  public TransactionalMessageProcessor(DestinationImpl destination,
      DestinationSubscriptionManager subscriptionManager,
      MessageManager messageManager, long bucketId) {
    super(destination, subscriptionManager, messageManager, bucketId);
  }

  @Override
  protected long processMessage(Message message) {
    destination.getStats().transactionalPublish();
    long delayed = message.getDelayed();
    if (delayed > 0) {
      delayed = System.currentTimeMillis() + delayed;
      message.setDelayed(delayed);
      destination.getDelayedStatus().register(delayed, message);
      return 0;
    } else {
      destination.getStats().messagePublished();
      return processSubscription(destination, subscriptionManager, message);
    }
  }
}
