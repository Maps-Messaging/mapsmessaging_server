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

package io.mapsmessaging.aggregator.worker;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class AggregatorWorkSchedulerTest {

  @Test
  void stripeCountZeroResolvesToPositive() {
    AggregatorWorkScheduler scheduler = new AggregatorWorkScheduler(0, 10, 1);
    assertNotNull(scheduler);
  }

  @Test
  void signalTriggersDrainAndIsIdempotent() throws Exception {
    AggregatorWorkScheduler scheduler = new AggregatorWorkScheduler(1, 10, 1);
    FakeWorkItem workItem = new FakeWorkItem("A", 100);

    scheduler.register(workItem);
    scheduler.start();

    try {
      for (int i = 0; i < 1000; i++) {
        scheduler.signal(workItem);
      }

      assertTrue(workItem.awaitDrained(100, 2, TimeUnit.SECONDS), "Work item did not drain");
      assertEquals(100, workItem.totalDrained.get(), "Expected all work to be drained");

      int drainCalls = workItem.drainCalls.get();
      Thread.sleep(50);
      assertTrue(drainCalls <= 20, "Too many drain calls, scheduler is thrashing: " + drainCalls);
    } finally {
      scheduler.stop();
    }
  }

  @Test
  void maxBatchLimitsDrainAndResignals() throws Exception {
    AggregatorWorkScheduler scheduler = new AggregatorWorkScheduler(1, 7, 1);
    FakeWorkItem workItem = new FakeWorkItem("B", 50);

    scheduler.register(workItem);
    scheduler.start();

    try {
      scheduler.signal(workItem);

      assertTrue(workItem.awaitDrained(50, 2, TimeUnit.SECONDS), "Work item did not drain enough");
      assertTrue(workItem.drainCalls.get() >= 8, "Expected multiple drain calls due to maxBatch");
      assertEquals(50, workItem.totalDrained.get());
    } finally {
      scheduler.stop();
    }
  }

  @Test
  void timeoutTickCallsCheckTimeout() throws Exception {
    AggregatorWorkScheduler scheduler = new AggregatorWorkScheduler(1, 10, 1);
    FakeWorkItem workItem = new FakeWorkItem("C", 0);

    scheduler.register(workItem);
    scheduler.start();

    try {
      long start = System.currentTimeMillis();
      while (workItem.timeoutCalls.get() == 0 && (System.currentTimeMillis() - start) < 500) {
        Thread.sleep(10);
      }
      assertTrue(workItem.timeoutCalls.get() > 0, "Expected checkTimeout() to be called by timeout tick");
    } finally {
      scheduler.stop();
    }
  }

  private static class FakeWorkItem implements AggregatorWorkItem {

    private final String name;
    private final AtomicInteger remainingWork;
    private final AtomicInteger totalDrained;
    private final AtomicInteger drainCalls;
    private final AtomicInteger timeoutCalls;
    private final AtomicInteger scheduled;
    private final CountDownLatch drainedLatch;

    private final AtomicLong lastDrainMillis;

    private FakeWorkItem(String name, int initialWork) {
      this.name = name;
      this.remainingWork = new AtomicInteger(initialWork);
      this.totalDrained = new AtomicInteger(0);
      this.drainCalls = new AtomicInteger(0);
      this.timeoutCalls = new AtomicInteger(0);
      this.scheduled = new AtomicInteger(0);
      this.drainedLatch = new CountDownLatch(initialWork == 0 ? 0 : 1);
      this.lastDrainMillis = new AtomicLong(0);
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public int drainOnce(int maxBatch) {
      drainCalls.incrementAndGet();
      lastDrainMillis.set(System.currentTimeMillis());

      int drained = 0;

      for (int i = 0; i < maxBatch; i++) {
        int remaining = remainingWork.get();
        if (remaining <= 0) {
          break;
        }
        if (remainingWork.compareAndSet(remaining, remaining - 1)) {
          drained++;
        }
      }

      if (drained > 0) {
        totalDrained.addAndGet(drained);
      }

      if (remainingWork.get() == 0 && drainedLatch.getCount() > 0) {
        drainedLatch.countDown();
      }

      return drained;
    }

    @Override
    public void checkTimeout() {
      timeoutCalls.incrementAndGet();
    }

    @Override
    public boolean tryMarkScheduled() {
      return scheduled.compareAndSet(0, 1);
    }

    @Override
    public void clearScheduled() {
      scheduled.set(0);
    }

    private boolean awaitDrained(int expected, long timeout, TimeUnit unit) throws InterruptedException {
      if (expected == 0) {
        return true;
      }
      boolean ok = drainedLatch.await(timeout, unit);
      return ok && totalDrained.get() == expected;
    }
  }
}
