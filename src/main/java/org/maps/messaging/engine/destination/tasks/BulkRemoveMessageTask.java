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

import java.util.Queue;
import org.maps.messaging.engine.destination.DestinationImpl;
import org.maps.messaging.engine.tasks.EngineTask;
import org.maps.messaging.engine.tasks.FutureResponse;
import org.maps.messaging.engine.tasks.LongResponse;
import org.maps.messaging.engine.tasks.Response;

public class BulkRemoveMessageTask extends EngineTask {
  private static final int MAX_TASK_TIME = 10000;

  private final Queue<Long> queue;
  private final DestinationImpl destination;

  public BulkRemoveMessageTask(DestinationImpl destination, Queue<Long> queue) {
    super();
    this.queue = queue;
    this.destination = destination;
  }

  @Override
  public Response taskCall() throws Exception {
    if(destination.isClosed()){
      return new LongResponse(-1);
    }
    int count = queue.size();
    long retained = destination.getRetainedIdentifier();
    long nano = System.nanoTime();
    long endTIme = System.currentTimeMillis() + MAX_TASK_TIME;
    while (!queue.isEmpty() && endTIme > System.currentTimeMillis()) {
      Long messageId = queue.poll();
      if (messageId != null && retained != messageId) {
        destination.removeMessage(messageId);
      }
    }
    int val = (count - queue.size());
    if (val != 0) {
      nano = ((System.nanoTime() - nano) / val) / 1000; // micro seconds
      destination.getStats().getDeleteTimeAverages().add(nano);
    }
    if (!queue.isEmpty()) {
      return new FutureResponse(destination.submit(new BulkRemoveMessageTask(destination, queue)));
    }
    return new LongResponse(count);
  }
}