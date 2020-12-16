/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.utilities.threads;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.junit.jupiter.api.Test;
import org.maps.utilities.threads.tasks.SingleConcurrentTaskScheduler;
import org.maps.utilities.threads.tasks.TaskScheduler;

public class TaskQueueTest {

  private static final AtomicBoolean run = new AtomicBoolean(false);

  @Test
  public void testConcurrentContention() {
    run.getAndSet(false);
    performTasks(new SingleConcurrentTaskScheduler("testTask"));
  }

  private void performTasks(TaskScheduler queue){
    LongAdder adder = new LongAdder();
    AtomicLong threadCount = new AtomicLong(0);

    Thread[] threads = new Thread[64];
    for (int x = 0; x < threads.length; x++) {
      threads[x] = new Thread(new TestRunner(queue, adder, threadCount));
      threads[x].start();
    }
    run.set(true);
    long previous = 0;
    long start = System.currentTimeMillis();
    while (threadCount.get() != 0) {
      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
        Thread.currentThread().interrupt();
      }
      long current = adder.sum();
      System.err.println(current - previous + " Threads:" + threadCount.get() + " Outstanding:" + queue.getOutstanding());
      previous = current;
    }
    System.err.println("Time to run:" + (System.currentTimeMillis() - start) + "ms, Summed " + adder.sum());
  }




  class Task implements Callable<Object> {
    private final LongAdder adder;
    public Task(LongAdder adder) {
      this.adder = adder;
    }

    @Override
    public Object call() throws Exception {
      adder.increment();
      return adder;
    }
  }

  final class TestRunner implements Runnable {

    TaskScheduler queue;
    AtomicLong threadCount;
    LongAdder adder;

    TestRunner(TaskScheduler queue, LongAdder adder, AtomicLong threadCount) {
      this.queue = queue;
      this.threadCount = threadCount;
      this.adder = adder;
    }

    public void run() {
      threadCount.incrementAndGet();
      while (!run.get()) {
        try {
          TimeUnit.MILLISECONDS.sleep(1);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      for (int x = 0; x < 100000; x++) {
        queue.addTask(new FutureTask<Object>(new Task(adder)));
        if(queue.getOutstanding() > 1000 && queue.getOutstanding() < 10000){
          Thread.yield();
        }
        if(queue.getOutstanding() > 10000){
          try {
            TimeUnit.MILLISECONDS.sleep(1);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
      threadCount.decrementAndGet();
    }
  }
}




