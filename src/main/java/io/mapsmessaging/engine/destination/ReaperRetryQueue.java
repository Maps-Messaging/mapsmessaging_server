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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** Failure-only retry state. Every access is owned by the destination's reaper lock. */
final class ReaperRetryQueue {

  private static final long RETRY_DELAY_NANOS = TimeUnit.SECONDS.toNanos(5);

  private final LongSupplier nanoClock;
  private final Map<Long, Long> pending = new LinkedHashMap<>();
  private final Set<Long> held = new LinkedHashSet<>();
  private long retryAfter;
  private boolean batchFailure;

  ReaperRetryQueue() {
    this(System::nanoTime);
  }

  ReaperRetryQueue(LongSupplier nanoClock) {
    this.nanoClock = nanoClock;
  }

  void hold(long messageId) {
    held.add(messageId);
  }

  void defer(Queue<Long> messageIds, boolean failedBatch) {
    long deadline = nanoClock.getAsLong() + RETRY_DELAY_NANOS;
    for (Long messageId : messageIds) {
      held.remove(messageId);
      pending.putIfAbsent(messageId, deadline);
    }
    if (failedBatch) {
      // A new failed attempt restarts the batch delay even if the previous deadline expired.
      retryAfter = deadline;
      batchFailure = true;
    }
  }

  boolean blocksFreshWork() {
    return batchFailure && nanoClock.getAsLong() - retryAfter < 0;
  }

  boolean hasReadyIds() {
    long now = nanoClock.getAsLong();
    for (long deadline : pending.values()) {
      if (now - deadline >= 0) {
        return true;
      }
    }
    return false;
  }

  void excludePending(Queue<Long> completed) {
    completed.removeAll(pending.keySet());
    completed.removeAll(held);
  }

  void drainReadyInto(Queue<Long> completed) {
    excludePending(completed);
    if (blocksFreshWork()) {
      return;
    }
    long now = nanoClock.getAsLong();
    Iterator<Map.Entry<Long, Long>> iterator = pending.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<Long, Long> entry = iterator.next();
      if (now - entry.getValue() >= 0) {
        completed.add(entry.getKey());
        iterator.remove();
      }
    }
    batchFailure = false;
  }

  String cleanupTraceState() {
    return "held=" + held + " pendingDeadlines=" + pending + " batchFailure=" + batchFailure + " retryAfter=" + retryAfter + " now=" + nanoClock.getAsLong();
  }

  void clear() {
    pending.clear();
    held.clear();
    batchFailure = false;
  }

}
