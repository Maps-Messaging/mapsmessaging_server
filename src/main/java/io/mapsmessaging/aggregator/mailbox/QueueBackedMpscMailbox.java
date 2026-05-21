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

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static io.mapsmessaging.logging.ServerLogMessages.AGGREGATOR_QUEUE_DRAIN;
import static io.mapsmessaging.logging.ServerLogMessages.AGGREGATOR_QUEUE_OFFER;

public class QueueBackedMpscMailbox<T> implements AggregatorMailbox<T> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final ArrayBlockingQueue<T> queue;

  private final AtomicLong offeredCount;

  public QueueBackedMpscMailbox(int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("capacity must be > 0");
    }
    this.queue = new ArrayBlockingQueue<>(capacity);
    this.offeredCount = new AtomicLong();
  }

  @Override
  public boolean offer(T item) {
    Objects.requireNonNull(item, "item must not be null");
    logger.log(AGGREGATOR_QUEUE_OFFER, item);
    this.offeredCount.incrementAndGet();
    return this.queue.offer(item);
  }

  @Override
  public int drainTo(Consumer<T> consumer, int maxItems) {
    Objects.requireNonNull(consumer, "consumer must not be null");
    logger.log(AGGREGATOR_QUEUE_DRAIN, maxItems);
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
}
