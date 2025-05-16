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
import io.mapsmessaging.engine.destination.delayed.MessageManager;
import io.mapsmessaging.engine.destination.subscription.DestinationSubscriptionManager;
import io.mapsmessaging.engine.tasks.LongResponse;
import io.mapsmessaging.engine.tasks.Response;

public abstract class MessageProcessor extends SubscriptionTask {

  protected final DestinationImpl destination;
  protected final MessageManager messageManager;
  protected final DestinationSubscriptionManager subscriptionManager;
  private final long bucketId;

  protected MessageProcessor(DestinationImpl destination, DestinationSubscriptionManager subscriptionManager, MessageManager messageManager, long bucketId) {
    super();
    this.destination = destination;
    this.messageManager = messageManager;
    this.subscriptionManager = subscriptionManager;
    this.bucketId = bucketId;
  }

  protected abstract long processMessage(Message message);

  @Override
  public Response taskCall() throws Exception {
    long counter = 0;
    while (true) {
      long messageId = messageManager.getNext(bucketId);
      if (messageId > -1) {
        Message message = destination.getMessage(messageId);
        if (message != null) {
          counter += processMessage(message);
        }
        messageManager.remove(bucketId, messageId);
      } else {
        break;
      }
    }
    return new LongResponse(counter);
  }
}
