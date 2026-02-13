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

package io.mapsmessaging.aggregator.mailbox;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class QueueBackedMpscMailbox<T> implements AggregatorMailbox<T> {

  private final ArrayBlockingQueue<T> queue;
  private final BackpressureMode backpressureMode;

  private final AtomicLong offeredCount;
  private final AtomicLong droppedCount;

  public QueueBackedMpscMailbox(int capacity, BackpressureMode backpressureMode) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("capacity must be > 0");
    }
    this.queue = new ArrayBlockingQueue<>(capacity);
    this.backpressureMode = Objects.requireNonNull(backpressureMode, "backpressureMode must not be null");

    this.offeredCount = new AtomicLong();
    this.droppedCount = new AtomicLong();
  }

  @Override
  public OfferOutcome offer(T item, Runnable onDrop) {
    Objects.requireNonNull(item, "item must not be null");

    this.offeredCount.incrementAndGet();

    boolean accepted = this.queue.offer(item);
    if (accepted) {
      return OfferOutcome.ACCEPTED;
    }

    this.droppedCount.incrementAndGet();

    if (this.backpressureMode == BackpressureMode.DROP_NEWEST) {
      if (onDrop != null) {
        safeRun(onDrop);
      }
      return OfferOutcome.DROPPED;
    }

    if (onDrop != null) {
      safeRun(onDrop);
    }
    return OfferOutcome.DROPPED;
  }

  @Override
  public T poll() {
    return this.queue.poll();
  }

  @Override
  public int drainTo(Consumer<T> consumer, int maxItems) {
    Objects.requireNonNull(consumer, "consumer must not be null");
    if (maxItems <= 0) {
      return 0;
    }

    int drained = 0;
    while (drained < maxItems) {
      T item = this.queue.poll();
      if (item == null) {
        break;
      }
      consumer.accept(item);
      drained++;
    }
    return drained;
  }

  @Override
  public int size() {
    return this.queue.size();
  }

  @Override
  public int capacity() {
    return this.queue.size() + this.queue.remainingCapacity();
  }

  @Override
  public long getOfferedCount() {
    return this.offeredCount.get();
  }

  @Override
  public long getDroppedCount() {
    return this.droppedCount.get();
  }

  private void safeRun(Runnable runnable) {
    try {
      runnable.run();
    } catch (Throwable ignored) {
      // Drop callback must never take down ingress.
    }
  }
}
