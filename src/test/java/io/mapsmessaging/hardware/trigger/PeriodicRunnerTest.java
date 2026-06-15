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

package io.mapsmessaging.hardware.trigger;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodicRunnerTest {

  @Test
  void run_doesNotQueueDuplicateWhileTaskIsRunning_andAllowsLaterRun() throws Exception {
    CountDownLatch firstStarted = new CountDownLatch(1);
    CountDownLatch releaseFirst = new CountDownLatch(1);
    CountDownLatch secondCompleted = new CountDownLatch(1);
    AtomicInteger invocationCount = new AtomicInteger();
    PeriodicRunner runner = new PeriodicRunner(() -> {
      int invocation = invocationCount.incrementAndGet();
      if (invocation == 1) {
        firstStarted.countDown();
        await(releaseFirst);
      }
      else {
        secondCompleted.countDown();
      }
    });

    try {
      runner.run();
      assertTrue(firstStarted.await(2, TimeUnit.SECONDS));

      runner.run();
      assertEquals(1, invocationCount.get());

      releaseFirst.countDown();
      runner.getSubmittedFuture().get(2, TimeUnit.SECONDS);

      runner.run();
      assertTrue(secondCompleted.await(2, TimeUnit.SECONDS));
      assertEquals(2, invocationCount.get());
    } finally {
      releaseFirst.countDown();
      runner.close();
    }
  }

  @Test
  void taskFailure_doesNotPreventLaterRun() throws Exception {
    CountDownLatch completed = new CountDownLatch(2);
    AtomicInteger invocationCount = new AtomicInteger();
    PeriodicRunner runner = new PeriodicRunner(() -> {
      completed.countDown();
      if (invocationCount.incrementAndGet() == 1) {
        throw new IllegalStateException("expected test failure");
      }
    });

    try {
      runner.run();
      runner.getSubmittedFuture().get(2, TimeUnit.SECONDS);
      runner.run();

      assertTrue(completed.await(2, TimeUnit.SECONDS));
      assertEquals(2, invocationCount.get());
    } finally {
      runner.close();
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
