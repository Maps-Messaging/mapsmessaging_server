/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.utilities.threads;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SimpleTaskSchedulerTest {

  @Test
  public void simpleScheduleTest(){
    SimpleTaskScheduler scheduler = SimpleTaskScheduler.getInstance();
    TestTask task = new TestTask();
    long time = System.currentTimeMillis();
    Future<?> future = scheduler.schedule(task, 1, TimeUnit.SECONDS);
    while(!future.isDone() && time+2000 >System.currentTimeMillis()){
      LockSupport.parkNanos(1000000);
    }
    time = task.executedTime - time;
    System.err.println(time);
    Assertions.assertTrue(time < 1600);
    Assertions.assertTrue(future.isDone());
    Assertions.assertFalse(future.isCancelled());
  }

  @Test
  public void simpleCancelTest(){
    SimpleTaskScheduler scheduler = SimpleTaskScheduler.getInstance();
    TestTask task = new TestTask();
    long time = System.currentTimeMillis();
    Future<?> future = scheduler.schedule(task, 2, TimeUnit.SECONDS);
    LockSupport.parkNanos(1000000);
    future.cancel(true);
    Assertions.assertFalse(future.isDone());
    Assertions.assertTrue(future.isCancelled());
  }

  private class TestTask implements Runnable{

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
