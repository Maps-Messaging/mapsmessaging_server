/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.utilities.queue;

import io.mapsmessaging.utilities.collections.NaturalOrderedLongQueue;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

public class EventReaperQueue {

  private final EventQueue[] localQueue;
  private final AtomicLong index;

  public EventReaperQueue() {
    index = new AtomicLong(0);
    int numberOfCPUs = Runtime.getRuntime().availableProcessors();
    localQueue = new EventQueue[numberOfCPUs];
    for (int x = 0; x < numberOfCPUs; x++) {
      localQueue[x] = new EventQueue();
    }
  }

  public void close() {
    for (EventQueue threadLocalQueue : localQueue) {
      threadLocalQueue.getAndClear();
    }
  }

  public void add(long id) {
    int idx = (int) index.incrementAndGet() % localQueue.length;
    localQueue[idx].add(id);
  }

  public Queue<Long> getAndClear() {
    if (Arrays.stream(localQueue).anyMatch(threadLocalQueue -> !threadLocalQueue.queue.isEmpty())) {
      Queue<Long> result = new NaturalOrderedLongQueue(0, new BitSetFactoryImpl(8192));
      for (EventQueue threadLocalQueue : localQueue) {
        Queue<Long> tQueue = threadLocalQueue.getAndClear();
        result.addAll(tQueue);
      }
      return result;
    }
    return new LinkedList<>();
  }

  private static final class EventQueue {

    private Queue<Long> queue;

    public EventQueue() {
      queue = new NaturalOrderedLongQueue(0, new BitSetFactoryImpl(8192));
    }

    public synchronized void add(long entry) {
      queue.offer(entry);
    }

    public synchronized Queue<Long> getAndClear() {
      Queue<Long> copy = queue;
      queue = new NaturalOrderedLongQueue(0, new BitSetFactoryImpl(8192));
      return copy;
    }
  }
}
