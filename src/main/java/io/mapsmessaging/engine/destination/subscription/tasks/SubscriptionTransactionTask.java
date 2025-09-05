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

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.engine.tasks.EngineTask;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.engine.tasks.VoidResponse;

public class SubscriptionTransactionTask extends EngineTask {


  private final SubscribedEventManager subscription;
  private final long messageId;
  private final boolean ack;

  public SubscriptionTransactionTask(SubscribedEventManager subscription, long messageId, final boolean ack) {
    this.subscription = subscription;
    this.messageId = messageId;
    this.ack = ack;
  }

  @Override
  public Response taskCall() {
    if (ack) {
      subscription.ackReceived(messageId);
    } else {
      subscription.rollbackReceived(messageId);
    }
    return new VoidResponse();
  }
}
