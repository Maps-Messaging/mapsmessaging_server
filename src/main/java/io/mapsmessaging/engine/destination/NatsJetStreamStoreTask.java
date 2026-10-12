/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.tasks.NonDelayedStoreMessageTask;

import java.io.IOException;

/**
 * NATS-specific persisted-identifier publish operation. Uses the normal MAPS
 * resource and subscription tasks, but retains the post-override Message so that
 * JetStream can acknowledge the actual assigned identifier.
 *
 * This does not alter DestinationImpl.storeMessage for other protocols.
 */
public final class NatsJetStreamStoreTask {

  private NatsJetStreamStoreTask() {
  }

  public static long store(DestinationImpl destination, Message message) throws IOException {
    if (message.isBound()) {
      throw new IOException("Message is bound to another destination");
    }
    if (destination.getResourceType() != DestinationType.TOPIC) {
      throw new IOException("JetStream can publish only to a MAPS topic");
    }
    Message stored = destination.applyMessageOverrides(message);
    if (stored.getDelayed() > 0) {
      throw new IOException("JetStream publication cannot be delayed before storage acknowledgement");
    }
    int interested = destination.handleTask(new NonDelayedStoreMessageTask(
        destination, destination.subscriptionManager, stored));
    long id = stored.getIdentifier();
    if (interested <= 0 || id <= 0) {
      throw new IOException("JetStream message was not retained by an archive subscription");
    }
    return id;
  }
}
