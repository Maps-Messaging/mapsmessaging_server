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

package org.maps.messaging.engine.destination.tasks;

import org.maps.messaging.engine.destination.DestinationImpl;
import org.maps.messaging.engine.tasks.EngineTask;
import org.maps.messaging.engine.tasks.LongResponse;
import org.maps.messaging.engine.tasks.Response;

public class RemoveMessageTask extends EngineTask {
  private final long messageId;
  private final DestinationImpl destination;

  public RemoveMessageTask(DestinationImpl destination, long messageId) {
    super();
    this.messageId = messageId;
    this.destination = destination;
  }

  @Override
  public Response taskCall() throws Exception {
    destination.removeMessage(messageId);
    return new LongResponse(1);
  }
}
