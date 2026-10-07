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

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.queue.EventReaperQueue;

import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Periodically removes messages that every subscription has finished with. Runs on a fixed-rate
 * schedule, where a single uncaught exception would cancel all later runs without any trace - and
 * the destination would then keep every message it ever receives. So a failed pass is logged, its
 * message ids are put back for the next pass, and the reaper carries on.
 */
final class CompletedMessageReaper implements Runnable {

  private final Logger logger = LoggerFactory.getLogger(CompletedMessageReaper.class);
  private final String destinationName;
  private final EventReaperQueue completionQueue;
  private final Supplier<Queue<Long>> interestedMessages;
  private final Consumer<Queue<Long>> removeMessages;

  private int countDown = 0;
  private int idleCount = 0;

  CompletedMessageReaper(String destinationName, EventReaperQueue completionQueue,
                         Supplier<Queue<Long>> interestedMessages, Consumer<Queue<Long>> removeMessages) {
    this.destinationName = destinationName;
    this.completionQueue = completionQueue;
    this.interestedMessages = interestedMessages;
    this.removeMessages = removeMessages;
  }

  @Override
  public void run() {
    if (countDown <= 0) {
      Queue<Long> completedQueue = completionQueue.getAndClear();
      if (!completedQueue.isEmpty()) {
        idleCount = 0;
        countDown = 0;
        reap(completedQueue);
      } else {
        idleCount = (idleCount + 1) % 20;
        countDown = 5 * idleCount;
      }
    }
    countDown--;
  }

  private void reap(Queue<Long> completedQueue) {
    try {
      completedQueue.removeAll(interestedMessages.get());
      if (!completedQueue.isEmpty()) {
        removeMessages.accept(completedQueue);
      }
    } catch (RuntimeException exception) {
      logger.log(ServerLogMessages.DESTINATION_REAPER_FAILED, exception, destinationName);
      for (Long messageId : completedQueue) {
        completionQueue.add(messageId);
      }
    }
  }
}
