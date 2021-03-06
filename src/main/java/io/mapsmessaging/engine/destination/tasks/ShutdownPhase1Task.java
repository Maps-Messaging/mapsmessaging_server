/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.engine.destination.tasks;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationManagerListener;
import io.mapsmessaging.engine.tasks.BooleanResponse;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.logging.Logger;
import java.util.List;

public class ShutdownPhase1Task extends StoreMessageTask {
  private final DestinationImpl destination;
  private final Logger logger;
  private final List<DestinationManagerListener> listener;

  public ShutdownPhase1Task(DestinationImpl destination, List<DestinationManagerListener> listeners, Logger logger){
    super();
    this.destination = destination;
    this.logger = logger;
    this.listener = listeners;
  }

  @Override
  public Response taskCall() {
    destination.pauseClientRequests();
    DeleteDestinationTask deleteDestinationTask = new DeleteDestinationTask(destination, listener, logger);
    destination.submit(deleteDestinationTask);
    return new BooleanResponse(true);
  }

}
