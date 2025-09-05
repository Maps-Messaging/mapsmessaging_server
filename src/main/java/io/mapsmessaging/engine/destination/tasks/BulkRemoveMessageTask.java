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
import io.mapsmessaging.engine.tasks.EngineTask;
import io.mapsmessaging.engine.tasks.FutureResponse;
import io.mapsmessaging.engine.tasks.LongResponse;
import io.mapsmessaging.engine.tasks.Response;

import java.util.Queue;

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
    if (destination.isClosed()) {
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
      destination.getStats().messageDeleteTime(nano);
    }
    if (!queue.isEmpty()) {
      return new FutureResponse(destination.submit(new BulkRemoveMessageTask(destination, queue)));
    }
    return new LongResponse(count);
  }
}