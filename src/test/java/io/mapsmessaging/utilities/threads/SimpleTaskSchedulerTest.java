/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.utilities.threads;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.mapsmessaging.test.WaitForState;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

class SimpleTaskSchedulerTest {

  @Test
  void simpleScheduleTest() throws IOException {
    SimpleTaskScheduler scheduler = SimpleTaskScheduler.getInstance();
    TestTask task = new TestTask();
    long time = System.currentTimeMillis();
    Future<?> future = scheduler.schedule(task, 1, TimeUnit.SECONDS);
    WaitForState.waitFor(2, TimeUnit.SECONDS, future::isDone);
    time = task.executedTime - time;
    Assertions.assertTrue(time < 1600);
    Assertions.assertTrue(future.isDone());
    Assertions.assertFalse(future.isCancelled());
    Assertions.assertTrue(task.executedTime > 0);
  }

  @Test
  void simpleScheduleRepeat() throws IOException {
    SimpleTaskScheduler scheduler = SimpleTaskScheduler.getInstance();
    TestTask task = new TestTask();
    long count = scheduler.getTotalScheduled();
    long executed = scheduler.getTotalExecuted();
    long depth = scheduler.getDepth();
    Future<?> future = scheduler.scheduleAtFixedRate(task, 1, 1, TimeUnit.SECONDS);
    WaitForState.waitFor(4, TimeUnit.SECONDS, future::isDone);
    Assertions.assertTrue(3 <= (scheduler.getTotalExecuted() - executed));
    future.cancel(true);
    Assertions.assertTrue(future.isCancelled());
    Assertions.assertTrue(count < scheduler.getTotalScheduled());
    count = scheduler.getTotalScheduled();
    WaitForState.waitFor(2, TimeUnit.SECONDS, future::isDone);
    Assertions.assertTrue(scheduler.getTotalScheduled() >= count);
  }

  @Test
  void simpleCancelTest(){
    SimpleTaskScheduler scheduler = SimpleTaskScheduler.getInstance();
    TestTask task = new TestTask();
    Future<?> future = scheduler.schedule(task, 2, TimeUnit.SECONDS);
    LockSupport.parkNanos(1000000);
    future.cancel(true);
    Assertions.assertFalse(future.isDone());
    Assertions.assertTrue(future.isCancelled());
  }

  @Test
  void simpleCancel() {
    SimpleTaskScheduler scheduler = SimpleTaskScheduler.getInstance();
    TestTask task = new TestTask();
    Future<?> future = scheduler.schedule(task, 1, TimeUnit.SECONDS);
    future.cancel(true);
    Assertions.assertFalse(future.isDone());
    Assertions.assertTrue(future.isCancelled());
    Assertions.assertEquals(-1, task.executedTime);
  }

  private static class TestTask implements Runnable{

    private long executedTime;

    public TestTask(){
      executedTime = -1;
    }

    @Override
    public void run() {
      executedTime = System.currentTimeMillis();
    }
  }
}
