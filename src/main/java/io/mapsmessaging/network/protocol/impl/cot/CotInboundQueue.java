/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.network.protocol.impl.cot;

import io.mapsmessaging.network.protocol.impl.cot.CotEchoSuppressor.CotEventInfo;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

final class CotInboundQueue implements AutoCloseable {

  private final int capacity;
  private final Executor executor;
  private final EventConsumer consumer;
  private final DroppedConsumer dropped;
  private final ErrorConsumer failed;
  private final Deque<Entry> queued;

  private boolean workerScheduled;
  private boolean closed;

  CotInboundQueue(
      int capacity,
      Executor executor,
      EventConsumer consumer,
      DroppedConsumer dropped,
      ErrorConsumer failed) {
    if (capacity < 1) {
      throw new IllegalArgumentException("capacity must be positive");
    }
    this.capacity = capacity;
    this.executor = executor;
    this.consumer = consumer;
    this.dropped = dropped;
    this.failed = failed;
    queued = new ArrayDeque<>();
  }

  OfferResult offer(byte[] xml, CotEventInfo event) {
    Entry removed = null;
    Entry rejected = null;
    boolean schedule = false;
    OfferResult result = OfferResult.ENQUEUED;
    synchronized (this) {
      Entry added = new Entry(xml, event);
      if (closed) {
        rejected = added;
        result = OfferResult.CLOSED;
      } else {
        removed = removeCoalesced(event);
        if (queued.size() >= capacity) {
          Entry lowValue = removeLowValue();
          if (lowValue != null && event.highValue()) {
            removed = lowValue;
          } else {
            if (lowValue != null) {
              queued.addFirst(lowValue);
            }
            rejected = added;
            result = OfferResult.DROPPED;
          }
        }
        if (rejected == null) {
          queued.addLast(added);
          result = removed == null ? OfferResult.ENQUEUED : OfferResult.COALESCED;
          if (!workerScheduled) {
            workerScheduled = true;
            schedule = true;
          }
        }
      }
    }
    if (removed != null) {
      dropped.accept(removed.xml(), removed.event());
    }
    if (rejected != null) {
      dropped.accept(rejected.xml(), rejected.event());
    }
    if (schedule) {
      try {
        executor.execute(this::drain);
      } catch (RejectedExecutionException exception) {
        rejectPendingAfterExecutorShutdown();
        return OfferResult.CLOSED;
      }
    }
    return result;
  }

  synchronized int size() {
    return queued.size();
  }

  @Override
  public void close() {
    synchronized (this) {
      closed = true;
      queued.clear();
    }
  }

  private void drain() {
    while (true) {
      Entry entry;
      synchronized (this) {
        entry = closed ? null : queued.pollFirst();
        if (entry == null) {
          workerScheduled = false;
          return;
        }
      }
      try {
        consumer.accept(entry.xml(), entry.event());
      } catch (Exception exception) {
        failed.accept(entry.event(), exception);
      }
    }
  }

  private void rejectPendingAfterExecutorShutdown() {
    Deque<Entry> pending = new ArrayDeque<>();
    synchronized (this) {
      closed = true;
      workerScheduled = false;
      pending.addAll(queued);
      queued.clear();
    }
    for (Entry entry : pending) {
      dropped.accept(entry.xml(), entry.event());
    }
  }

  private Entry removeCoalesced(CotEventInfo event) {
    if (!event.coalescible()) {
      return null;
    }
    Iterator<Entry> iterator = queued.iterator();
    while (iterator.hasNext()) {
      Entry candidate = iterator.next();
      if (candidate.event().coalescible()
          && candidate.event().uid().equals(event.uid())) {
        iterator.remove();
        return candidate;
      }
    }
    return null;
  }

  private Entry removeLowValue() {
    Iterator<Entry> iterator = queued.iterator();
    while (iterator.hasNext()) {
      Entry candidate = iterator.next();
      if (!candidate.event().highValue()) {
        iterator.remove();
        return candidate;
      }
    }
    return null;
  }

  enum OfferResult {
    ENQUEUED,
    COALESCED,
    DROPPED,
    CLOSED
  }

  @FunctionalInterface
  interface EventConsumer {
    void accept(byte[] xml, CotEventInfo event) throws Exception;
  }

  @FunctionalInterface
  interface ErrorConsumer {
    void accept(CotEventInfo event, Exception exception);
  }

  @FunctionalInterface
  interface DroppedConsumer {
    void accept(byte[] xml, CotEventInfo event);
  }

  private record Entry(byte[] xml, CotEventInfo event) {
  }
}
