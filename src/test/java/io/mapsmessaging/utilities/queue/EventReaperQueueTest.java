/*
 *
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

package io.mapsmessaging.utilities.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class EventReaperQueueTest {

  @Test
  void getAndClearTransfersBatchWithoutLosingSubsequentCompletions() {
    EventReaperQueue queue = new EventReaperQueue();

    assertTrue(queue.isEmpty());
    for (long id = 1; id <= 1_000; id++) {
      queue.add(id);
    }

    Queue<Long> first = queue.getAndClear();
    assertEquals(1_000, first.size());
    assertTrue(queue.isEmpty());

    queue.add(1_001);
    assertEquals(1_000, first.size());
    assertFalse(queue.isEmpty());

    Queue<Long> second = queue.getAndClear();
    assertEquals(Set.of(1_001L), new HashSet<>(second));
    assertTrue(queue.isEmpty());
  }

  @Test
  void concurrentCompletionsAreRetainedAcrossBatchSwaps() throws Exception {
    EventReaperQueue queue = new EventReaperQueue();
    int writers = 4;
    int perWriter = 1_000;
    CountDownLatch start = new CountDownLatch(1);

    try (var executor = Executors.newFixedThreadPool(writers)) {
      var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
      for (int writer = 0; writer < writers; writer++) {
        final int writerId = writer;
        futures.add(executor.submit(() -> {
          start.await();
          for (int i = 1; i <= perWriter; i++) {
            queue.add((long) writerId * perWriter + i);
          }
          return null;
        }));
      }
      start.countDown();

      Set<Long> received = new HashSet<>();
      while (futures.stream().anyMatch(future -> !future.isDone())) {
        received.addAll(queue.getAndClear());
        Thread.yield();
      }
      for (var future : futures) {
        future.get(10, TimeUnit.SECONDS);
      }
      received.addAll(queue.getAndClear());

      assertEquals(writers * perWriter, received.size());
      assertTrue(queue.isEmpty());
    }
  }
}
