/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

package org.maps.messaging.engine.destination.tasks;

import java.io.IOException;
import java.util.List;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.messaging.engine.destination.DestinationImpl;
import org.maps.messaging.engine.destination.DestinationManagerListener;
import org.maps.messaging.engine.tasks.BooleanResponse;
import org.maps.messaging.engine.tasks.Response;

public class DeleteDestinationTask extends StoreMessageTask {
  private final DestinationImpl destination;
  private final Logger logger;
  private final List<DestinationManagerListener> listeners;

  public DeleteDestinationTask(DestinationImpl destination, List<DestinationManagerListener> listeners, Logger logger){
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
      logger.log(LogMessages.DESTINATION_MANAGER_DELETED_TOPIC,  destination.getName(), e);
    }
    for (DestinationManagerListener listener : listeners) {
      listener.deleted(destination);
    }
    logger.log(LogMessages.DESTINATION_MANAGER_DELETED_TOPIC, destination.getName());
    return new BooleanResponse(true);
  }

}
