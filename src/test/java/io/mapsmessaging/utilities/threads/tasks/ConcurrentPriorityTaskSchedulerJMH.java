/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.utilities.threads.tasks;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;

@State(Scope.Benchmark)
public class ConcurrentPriorityTaskSchedulerJMH {
  PriorityTaskScheduler<Object> queue;
  LongAdder adder;
  AtomicLong priority = new AtomicLong(0);

  @Setup
  public void createState(){
    System.err.println("New Queue created");
    queue = new PriorityConcurrentTaskScheduler<>("Benchmark", 32);
    adder = new LongAdder();
  }

  @TearDown
  public void cleanUp(){
    System.err.println("Max Outstanding:"+queue.getMaxOutstanding()+" Total Tasks:"+queue.getTotalTasksQueued()+" Offloaded:"+queue.getOffloadCount()+" Adder:"+adder.sum());
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @Threads(64)
  public void performTasks(){
    FutureTask<Object> futureTask = new FutureTask<>(new Task());
    queue.addTask(futureTask, (int)(priority.incrementAndGet() % 32));
    if(queue.getOutstanding() > 10000) {
      while (!futureTask.isDone()) {
        LockSupport.parkNanos(1);
      }
    }
  }

  class Task implements Callable<Object> {
    @Override
    public Object call() {
      adder.increment();
      return adder;
    }
  }
}
