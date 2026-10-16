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
import io.mapsmessaging.engine.destination.QueueCleanupTrace;
import io.mapsmessaging.engine.tasks.EngineTask;
import io.mapsmessaging.engine.tasks.FutureResponse;
import io.mapsmessaging.engine.tasks.LongResponse;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public class BulkRemoveMessageTask extends EngineTask {

  private static final Logger logger = LoggerFactory.getLogger(BulkRemoveMessageTask.class);
  private static final long MAX_TASK_TIME_NANOS = TimeUnit.SECONDS.toNanos(10);

  private final Queue<Long> queue;
  private final Queue<Long> failedMessages;
  private final DestinationImpl destination;
  private final LongSupplier nanoClock;

  public BulkRemoveMessageTask(DestinationImpl destination, Queue<Long> queue) {
    this(destination, queue, System::nanoTime);
  }

  BulkRemoveMessageTask(DestinationImpl destination, Queue<Long> queue, LongSupplier nanoClock) {
    this(destination, queue, new ArrayDeque<>(), nanoClock);
  }

  private BulkRemoveMessageTask(DestinationImpl destination, Queue<Long> queue, Queue<Long> failedMessages, LongSupplier nanoClock) {
    this.queue = queue;
    this.destination = destination;
    this.failedMessages = failedMessages;
    this.nanoClock = nanoClock;
  }

  @Override
  public Response taskCall() throws Exception {
    if (destination.isClosed()) {
      return new LongResponse(-1);
    }
    try {
      if (!destination.prepareCompletedRemoval(queue)) {
        deferRemaining(true);
        return new LongResponse(0);
      }
      return removeBatch();
    } catch (Exception failure) {
      deferRemaining(true);
      logger.log(ServerLogMessages.DESTINATION_REAPER_FAILED, failure, destination.getFullyQualifiedNamespace(), "bulk removal/continuation");
      throw failure;
    }
  }

  private Response removeBatch() throws Exception {
    int removed = 0;
    long retained = destination.getRetainedIdentifier();
    long nano = System.nanoTime();
    long started = nanoClock.getAsLong();
    while (!queue.isEmpty() && nanoClock.getAsLong() - started < MAX_TASK_TIME_NANOS) {
      Long messageId = queue.peek();
      if (messageId != null && retained != messageId) {
        try {
          destination.removeMessage(messageId);
          removed++;
        } catch (IOException | RuntimeException failure) {
          QueueCleanupTrace.record(destination.getFullyQualifiedNamespace(), "REMOVE-FAILED", messageId, failure.toString());
          failedMessages.add(messageId);
          destination.holdCompletedMessage(messageId);
          logger.log(ServerLogMessages.DESTINATION_REAPER_REMOVE_FAILED, failure, destination.getFullyQualifiedNamespace(), messageId);
        }
      }
      queue.poll();
      if (failedMessages.size() >= 2) {
        // Two failures classify this as a batch problem; never immediately resubmit it.
        deferRemaining(true);
        return new LongResponse(removed);
      }
    }
    if (removed != 0) {
      destination.getStats().messageDeleteTime((System.nanoTime() - nano) / removed / 1000);
    }
    if (!queue.isEmpty()) {
      // Keep the first failure across time slices so a second failure still stops this batch.
      return new FutureResponse(destination.submit(new BulkRemoveMessageTask(destination, queue, failedMessages, nanoClock)));
    }
    if (!failedMessages.isEmpty()) {
      destination.deferCompletedMessages(failedMessages, false);
    }
    return new LongResponse(removed);
  }

  private void deferRemaining(boolean batchFailure) {
    Queue<Long> retry = new ArrayDeque<>(failedMessages);
    retry.addAll(queue);
    destination.deferCompletedMessages(retry, batchFailure);
  }
}
