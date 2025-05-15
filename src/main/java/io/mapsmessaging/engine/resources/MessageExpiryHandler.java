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

package io.mapsmessaging.engine.resources;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.tasks.BulkRemoveMessageTask;
import io.mapsmessaging.storage.ExpiredStorableHandler;
import java.io.IOException;
import java.util.Queue;
import lombok.Setter;

public class MessageExpiryHandler implements ExpiredStorableHandler {

  private @Setter DestinationImpl destination;

  public MessageExpiryHandler() {
  }

  public MessageExpiryHandler(DestinationImpl destination) {
    this.destination = destination;
  }

  @Override
  public void expired(Queue<Long> queue) throws IOException {
    destination.handleTask(new BulkRemoveMessageTask(destination, queue));
  }

}
