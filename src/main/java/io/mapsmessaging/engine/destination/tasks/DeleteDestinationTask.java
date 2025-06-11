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

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationUpdateManager;
import io.mapsmessaging.engine.tasks.BooleanResponse;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.ServerLogMessages;

import java.io.IOException;

public class DeleteDestinationTask extends StoreMessageTask {

  private final DestinationImpl destination;
  private final Logger logger;
  private final DestinationUpdateManager listeners;

  public DeleteDestinationTask(DestinationImpl destination, DestinationUpdateManager listeners, Logger logger) {
    super();
    this.destination = destination;
    this.logger = logger;
    this.listeners = listeners;
  }

  @Override
  public Response taskCall() {
    destination.stopSubscriptions();
    try {
      destination.delete();
    } catch (IOException e) {
      logger.log(ServerLogMessages.DESTINATION_MANAGER_DELETED_TOPIC, destination.getFullyQualifiedNamespace(), e);
    }
    listeners.deleted(destination);
    logger.log(ServerLogMessages.DESTINATION_MANAGER_DELETED_TOPIC, destination.getFullyQualifiedNamespace());
    return new BooleanResponse(true);
  }

}
