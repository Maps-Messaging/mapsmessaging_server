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

import io.mapsmessaging.engine.destination.subscription.impl.shared.SharedSubscription;
import io.mapsmessaging.engine.destination.subscription.transaction.AcknowledgementController;
import io.mapsmessaging.engine.tasks.EngineTask;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.engine.tasks.VoidResponse;

public class SharedSubscriptionTask extends EngineTask {

  private final SharedSubscription sharedSubscription;
  private final AcknowledgementController acknowledgementController;
  private final long messageId;
  private final boolean ack;

  public SharedSubscriptionTask(SharedSubscription sharedSubscription, AcknowledgementController acknowledgementController, long messageId, final boolean ack) {
    this.sharedSubscription = sharedSubscription;
    this.acknowledgementController = acknowledgementController;
    this.messageId = messageId;
    this.ack = ack;
  }

  @Override
  public Response taskCall() {
    if (ack) {
      acknowledgementController.ack(messageId);
      sharedSubscription.ackReceived(messageId);
    } else {
      acknowledgementController.rollback(messageId);
    }
    return new VoidResponse();
  }
}
