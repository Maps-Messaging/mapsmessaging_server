/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.messaging.engine.destination.subscription.tasks;

import org.maps.messaging.engine.destination.subscription.impl.shared.SharedSubscription;
import org.maps.messaging.engine.destination.subscription.transaction.AcknowledgementController;
import org.maps.messaging.engine.tasks.EngineTask;
import org.maps.messaging.engine.tasks.Response;
import org.maps.messaging.engine.tasks.VoidResponse;

public class SharedSubscriptionTask extends EngineTask {
  private final SharedSubscription sharedSubscription;
  private final AcknowledgementController acknowledgementController;
  private final long messageId;
  private final boolean ack;

  public SharedSubscriptionTask(SharedSubscription sharedSubscription, AcknowledgementController acknowledgementController, long messageId, final boolean ack){
    this.sharedSubscription = sharedSubscription;
    this.acknowledgementController = acknowledgementController;
    this.messageId = messageId;
    this.ack = ack;
  }

  @Override
  public Response taskCall() {
    if(ack){
      acknowledgementController.ack(messageId);
      sharedSubscription.ackReceived(messageId);
    }
    else{
      acknowledgementController.rollback(messageId);
    }
    return new VoidResponse();
  }
}
