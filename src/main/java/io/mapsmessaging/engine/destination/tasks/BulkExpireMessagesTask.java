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

package io.mapsmessaging.engine.destination.tasks;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.tasks.EngineTask;
import io.mapsmessaging.engine.tasks.FutureResponse;
import io.mapsmessaging.engine.tasks.LongResponse;
import io.mapsmessaging.engine.tasks.Response;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Future;

/** Forced TTL/capacity removal; ordinary ACK-driven removal stays separate. */
public final class BulkExpireMessagesTask extends EngineTask {

  private static final int MAX_BATCH_SIZE = 1000;

  private final DestinationImpl destination;
  private final Queue<Long> expired;

  public BulkExpireMessagesTask(DestinationImpl destination, Queue<Long> expired) {
    this.destination = destination;
    this.expired = expired;
  }

  @Override
  public Response taskCall() throws Exception {
    if (destination.isClosed()) {
      return new LongResponse(-1);
    }
    Queue<Long> removed = new ArrayDeque<>();
    Future<Response> notification = null;
    try {
      while (!expired.isEmpty() && removed.size() < MAX_BATCH_SIZE) {
        long id = expired.remove();
        // Forced expiry also removes retained payloads. removeMessage maintains
        // the retained pointer using the ordinary resource operation.
        destination.removeMessage(id);
        removed.add(id);
      }
    } finally {
      if (!removed.isEmpty()) {
        notification = destination.expireSubscriptions(removed);
      }
    }
    if (!expired.isEmpty()) {
      return new FutureResponse(destination.submit(
          new BulkExpireMessagesTask(destination, expired), DestinationImpl.DELETE_PRIORITY));
    }
    return notification == null ? new LongResponse(0) : new FutureResponse(notification);
  }
}
