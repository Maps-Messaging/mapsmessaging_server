/*
 *
 *  Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *  Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.engine.destination.subscription.impl;

import io.mapsmessaging.engine.destination.subscription.tasks.MessageDeliveredListener;
import io.mapsmessaging.engine.destination.subscription.transaction.AcknowledgementController;

public class MessageDeliveryCompletionTask implements Runnable {

  private final DestinationSubscription subscription;
  private final AcknowledgementController acknowledgementController;

  public MessageDeliveryCompletionTask(DestinationSubscription subscription, AcknowledgementController acknowledgementController) {
    this.subscription = subscription;
    this.acknowledgementController = acknowledgementController;
  }

  @Override
  public void run() {
    subscription.getDestinationImpl().submit(new MessageDeliveredListener(subscription, acknowledgementController));
  }
}
