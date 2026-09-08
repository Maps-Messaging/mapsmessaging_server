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

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.protocol.impl.cot.CotEchoSuppressor.CotEventInfo;
import java.net.SocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class CotOutboundQueue implements AutoCloseable {

  private final int capacity;
  private final int chunkSize;
  private final Consumer<ServerPacket> sink;
  private final Predicate<CotEventInfo> stillValid;
  private final Consumer<CotEventInfo> dispatched;
  private final Consumer<CotEventInfo> completed;
  private final Consumer<CotEventInfo> dropped;
  private final Clock clock;
  private final Deque<Entry> queued;

  private Entry inFlight;
  private Instant inFlightSince;
  private boolean closed;

  CotOutboundQueue(
      int capacity,
      int chunkSize,
      Consumer<ServerPacket> sink,
      Predicate<CotEventInfo> stillValid,
      Consumer<CotEventInfo> dispatched,
      Consumer<CotEventInfo> completed,
      Consumer<CotEventInfo> dropped,
      Clock clock) {
    if (capacity < 1) {
      throw new IllegalArgumentException("capacity must be positive");
    }
    if (chunkSize < 1) {
      throw new IllegalArgumentException("chunkSize must be positive");
    }
    this.capacity = capacity;
    this.chunkSize = chunkSize;
    this.sink = sink;
    this.stillValid = stillValid;
    this.dispatched = dispatched;
    this.completed = completed;
    this.dropped = dropped;
    this.clock = clock;
    queued = new ArrayDeque<>();
  }

  OfferResult offer(byte[] bytes, CotEventInfo event, Runnable completion) {
    List<Entry> removed = new ArrayList<>();
    Entry dispatch = null;
    OfferResult result = OfferResult.ENQUEUED;
    synchronized (this) {
      Entry added = new Entry(bytes, event, completion);
      if (closed) {
        removed.add(added);
        result = OfferResult.CLOSED;
      } else {
        Entry coalesced = removeCoalesced(event);
        if (coalesced != null) {
          removed.add(coalesced);
        }
        if (sizeLocked() >= capacity) {
          Entry evicted = removeLowValue();
          if (evicted != null && (event.highValue() || event.coalescible())) {
            removed.add(evicted);
          } else {
            if (evicted != null) {
              queued.addFirst(evicted);
            }
            removed.add(added);
            result = OfferResult.DROPPED;
          }
        }
        if (result != OfferResult.DROPPED) {
          queued.addLast(added);
          result = coalesced == null ? OfferResult.ENQUEUED : OfferResult.COALESCED;
        }
      }
      dispatch = takeNextLocked(removed);
    }
    completeDropped(removed);
    dispatch(dispatch);
    return result;
  }

  synchronized int size() {
    return sizeLocked();
  }

  synchronized boolean isWriteTimedOut(Instant now, Duration timeout) {
    return inFlightSince != null && !now.isBefore(inFlightSince.plus(timeout));
  }

  @Override
  public void close() {
    List<Entry> pending = new ArrayList<>();
    synchronized (this) {
      if (closed) {
        return;
      }
      closed = true;
      if (inFlight != null) {
        pending.add(inFlight);
        inFlight = null;
        inFlightSince = null;
      }
      pending.addAll(queued);
      queued.clear();
    }
    completeDropped(pending);
  }

  private void frameCompleted(Entry entry) {
    Entry dispatch;
    List<Entry> removed = new ArrayList<>();
    boolean wasCurrent;
    synchronized (this) {
      wasCurrent = inFlight == entry;
      if (wasCurrent) {
        inFlight = null;
        inFlightSince = null;
      }
      dispatch = takeNextLocked(removed);
    }
    completeDropped(removed);
    if (wasCurrent && entry.completeOnce()) {
      completed.accept(entry.event());
    }
    dispatch(dispatch);
  }

  private Entry takeNextLocked(List<Entry> removed) {
    if (closed || inFlight != null) {
      return null;
    }
    Entry candidate;
    while ((candidate = queued.pollFirst()) != null) {
      if (stillValid.test(candidate.event())) {
        inFlight = candidate;
        inFlightSince = clock.instant();
        return candidate;
      }
      removed.add(candidate);
    }
    return null;
  }

  private void dispatch(Entry entry) {
    if (entry == null) {
      return;
    }
    dispatched.accept(entry.event());
    byte[] bytes = entry.bytes();
    for (int offset = 0; offset < bytes.length; offset += chunkSize) {
      int end = Math.min(bytes.length, offset + chunkSize);
      Runnable completion = end == bytes.length ? () -> frameCompleted(entry) : () -> { };
      sink.accept(new CotFrame(Arrays.copyOfRange(bytes, offset, end), completion));
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

  private void completeDropped(List<Entry> entries) {
    for (Entry entry : entries) {
      if (entry.completeOnce()) {
        dropped.accept(entry.event());
      }
    }
  }

  private int sizeLocked() {
    return queued.size() + (inFlight == null ? 0 : 1);
  }

  enum OfferResult {
    ENQUEUED,
    COALESCED,
    DROPPED,
    CLOSED
  }

  private static final class Entry {

    private final byte[] bytes;
    private final CotEventInfo event;
    private final Runnable completion;
    private final AtomicBoolean completed;

    private Entry(byte[] bytes, CotEventInfo event, Runnable completion) {
      this.bytes = bytes;
      this.event = event;
      this.completion = completion;
      completed = new AtomicBoolean();
    }

    byte[] bytes() {
      return bytes;
    }

    CotEventInfo event() {
      return event;
    }

    boolean completeOnce() {
      if (!completed.compareAndSet(false, true)) {
        return false;
      }
      if (completion != null) {
        completion.run();
      }
      return true;
    }
  }

  private static final class CotFrame implements ServerPacket {

    private final byte[] bytes;
    private final Runnable completion;

    private CotFrame(byte[] bytes, Runnable completion) {
      this.bytes = bytes;
      this.completion = completion;
    }

    @Override
    public int packFrame(Packet packet) {
      packet.put(bytes);
      return bytes.length;
    }

    @Override
    public void complete() {
      completion.run();
    }

    @Override
    public SocketAddress getFromAddress() {
      return null;
    }
  }
}
