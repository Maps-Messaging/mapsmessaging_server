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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class QueueBackedMpscMailboxTest {

  @Test
  void offerHonoursCapacity() {
    QueueBackedMpscMailbox<Integer> mailbox = new QueueBackedMpscMailbox<>(3);

    assertTrue(mailbox.offer(1));
    assertTrue(mailbox.offer(2));
    assertTrue(mailbox.offer(3));
    assertFalse(mailbox.offer(4));
  }

  @Test
  void drainToHonoursMaxBatch() {
    QueueBackedMpscMailbox<Integer> mailbox = new QueueBackedMpscMailbox<>(10);

    assertTrue(mailbox.offer(1));
    assertTrue(mailbox.offer(2));
    assertTrue(mailbox.offer(3));
    assertTrue(mailbox.offer(4));

    List<Integer> drained = new ArrayList<>();
    int drainedCount = mailbox.drainTo(drained::add, 2);

    assertEquals(2, drainedCount);
    assertEquals(List.of(1, 2), drained);

    drained.clear();
    drainedCount = mailbox.drainTo(drained::add, 10);

    assertEquals(2, drainedCount);
    assertEquals(List.of(3, 4), drained);
  }

  @Test
  void concurrentProducersNoLoss() throws Exception {
    int producerCount = 8;
    int perProducer = 5000;
    int total = producerCount * perProducer;

    QueueBackedMpscMailbox<Integer> mailbox = new QueueBackedMpscMailbox<>(total + 10);

    ExecutorService producers = Executors.newFixedThreadPool(producerCount);
    CountDownLatch startLatch = new CountDownLatch(1);

    for (int producerIndex = 0; producerIndex < producerCount; producerIndex++) {
      int base = producerIndex * perProducer;
      producers.submit(() -> {
        startLatch.await();
        for (int i = 0; i < perProducer; i++) {
          boolean accepted = mailbox.offer(base + i);
          if (!accepted) {
            fail("Mailbox rejected item unexpectedly; capacity too small or offer semantics incorrect");
          }
        }
        return null;
      });
    }

    startLatch.countDown();
    producers.shutdown();
    assertTrue(producers.awaitTermination(10, TimeUnit.SECONDS));

    AtomicInteger drainedCount = new AtomicInteger(0);

    int pass;
    do {
      pass = mailbox.drainTo(value -> drainedCount.incrementAndGet(), 2048);
    } while (pass > 0);

    assertEquals(total, drainedCount.get());
  }

  @Test
  public void drainToOnEmptyReturnsZeroAndDoesNotInvokeConsumer() {
    QueueBackedMpscMailbox<Integer> mailbox = new QueueBackedMpscMailbox<>(10);

    AtomicInteger calls = new AtomicInteger(0);
    int drained = mailbox.drainTo(v -> calls.incrementAndGet(), 10);

    assertEquals(0, drained);
    assertEquals(0, calls.get());
  }

  @Test
  public void drainToMaxBatchZeroDrainsNothing() {
    QueueBackedMpscMailbox<Integer> mailbox = new QueueBackedMpscMailbox<>(10);

    assertTrue(mailbox.offer(1));
    assertTrue(mailbox.offer(2));

    List<Integer> drained = new ArrayList<>();
    int count = mailbox.drainTo(drained::add, 0);

    assertEquals(0, count);
    assertTrue(drained.isEmpty());

    // items should still be there
    count = mailbox.drainTo(drained::add, 10);
    assertEquals(2, count);
    assertEquals(List.of(1, 2), drained);
  }

  @Test
  public void singleProducerIsFifo() {
    QueueBackedMpscMailbox<Integer> mailbox = new QueueBackedMpscMailbox<>(100);

    for (int i = 1; i <= 50; i++) {
      assertTrue(mailbox.offer(i));
    }

    List<Integer> drained = new ArrayList<>();
    int count = mailbox.drainTo(drained::add, 100);

    assertEquals(50, count);
    for (int i = 1; i <= 50; i++) {
      assertEquals(i, drained.get(i - 1));
    }
  }

  @Test
  void interleavedDrainDoesNotLoseItems() throws Exception {
    int total = 20_000;
    QueueBackedMpscMailbox<Integer> mailbox = new QueueBackedMpscMailbox<>(total + 10);

    ExecutorService producer = Executors.newSingleThreadExecutor();
    CountDownLatch startLatch = new CountDownLatch(1);

    producer.submit(() -> {
      startLatch.await();
      for (int i = 0; i < total; i++) {
        if (!mailbox.offer(i)) {
          fail("Unexpected rejection");
        }
      }
      return null;
    });

    startLatch.countDown();

    AtomicInteger drainedCount = new AtomicInteger(0);
    while (drainedCount.get() < total) {
      int pass = mailbox.drainTo(v -> drainedCount.incrementAndGet(), 123);
      if (pass == 0) {
        Thread.yield();
      }
    }

    producer.shutdown();
    assertTrue(producer.awaitTermination(5, TimeUnit.SECONDS));
    assertEquals(total, drainedCount.get());
  }

  @Test
  void sizeAndOfferedCountSingleThreaded() {
    int capacity = 10;
    QueueBackedMpscMailbox<Integer> mailbox = new QueueBackedMpscMailbox<>(capacity);

    assertEquals(0, mailbox.size());
    assertEquals(capacity, mailbox.capacity());
    assertEquals(0, mailbox.getOfferedCount());

    assertTrue(mailbox.offer(1));
    assertTrue(mailbox.offer(2));
    assertTrue(mailbox.offer(3));

    assertEquals(3, mailbox.size());
    assertEquals(3, mailbox.getOfferedCount());

    List<Integer> drained = new ArrayList<>();
    int drainedCount = mailbox.drainTo(drained::add, 10);

    assertEquals(3, drainedCount);
    assertEquals(0, mailbox.size());
    assertEquals(3, mailbox.getOfferedCount()); // offered count should NOT decrease
  }

}
