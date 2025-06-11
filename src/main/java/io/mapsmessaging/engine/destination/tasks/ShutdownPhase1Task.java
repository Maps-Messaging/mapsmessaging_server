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

import static io.mapsmessaging.engine.destination.DestinationImpl.TASK_QUEUE_PRIORITY_SIZE;

public class ShutdownPhase1Task extends StoreMessageTask {

  private final DestinationImpl destination;
  private final Logger logger;
  private final DestinationUpdateManager listener;

  public ShutdownPhase1Task(DestinationImpl destination, DestinationUpdateManager listeners, Logger logger) {
    super();
    this.destination = destination;
    this.logger = logger;
    this.listener = listeners;
  }

  @Override
  public Response taskCall() {
    destination.pauseClientRequests();
    DeleteDestinationTask deleteDestinationTask = new DeleteDestinationTask(destination, listener, logger);
    deleteDestinationTask.taskCall();
    destination.submit(deleteDestinationTask, TASK_QUEUE_PRIORITY_SIZE - 1);
    return new BooleanResponse(true);
  }

}
