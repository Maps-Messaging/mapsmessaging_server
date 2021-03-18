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

import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

/**
 * This class uses the Java Executor classes to make a simple task scheduler singleton. It offers simple scheduled at set intervals as well as a once only execution queue.
 * Keeping it all in one class simplifies the usage of task execution to one instance, so, instead of having multiple executors we have 1 that can be used by all.
 * This keeps the config simple and reuse helps with code stability. It also exposes the statistics via a JMX Bean for monitoring
 *
 * Refer to java.util.concurrent.Future for documentation on how to use a Futures
 *
 * @since 1.0
 * @author Matthew Buckton
 * @version 1.0
 */
@ToString
public class SimpleTaskScheduler {


  private static final long TICK_PERIOD = 100;
  private static final SimpleTaskScheduler instance = new SimpleTaskScheduler();

  private final ConcurrentNavigableMap<Long, ScheduledFuture> scheduledTasks;
  private final ExecutorService executor;
  private final LongAdder totalScheduled;
  private final LongAdder totalExecuted;

  private SimpleTaskScheduler() {
    totalScheduled = new LongAdder();
    totalExecuted = new LongAdder();
    scheduledTasks = new ConcurrentSkipListMap<>();
    executor = Executors.newWorkStealingPool();
    ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
    scheduler.setRemoveOnCancelPolicy(true);
    scheduler.scheduleAtFixedRate(new TaskExecutor(), TICK_PERIOD, TICK_PERIOD, TimeUnit.MILLISECONDS);
  }

  /**
   * Get the instance of the scheduler
   * @return the single instance of the Task Scheduler
   */
  public static SimpleTaskScheduler getInstance() {
    return instance;
  }

  /**
   * Submit a Runnable task for single execution as soon as possible.
   * @param runnable code to execute
   * @return Future object so the caller can monitor when the task is complete, also allows the task to be cancelled
   */
  // Will come back here but this will do for now
  @java.lang.SuppressWarnings("squid:S1452")
  public Future<?> submit(@NonNull @NotNull Runnable runnable) {
    totalScheduled.increment();
    return executor.submit(runnable);
  }

  /**
   * Schedules a single task to start after delay and then repeatably after every interval. The only way to stop it is to cancel via the Future object that
   * was returned
   *
   * @param runnable The task to execute
   * @param delay The initial delay before the first execution occurs
   * @param interval The time between future execution
   * @param unit The time unit that delay and interval are specified in
   * @return The future object that maintains the state of this runnable
   */
  public Future<Runnable> scheduleAtFixedRate(@NonNull @NotNull Runnable runnable, long delay, long interval,@NonNull @NotNull  TimeUnit unit) {
    long delayNano = System.nanoTime() + unit.toNanos(delay);
    long intervalNano = unit.toNanos(interval);
    totalScheduled.increment();
    return addTask(delayNano,  new ScheduledFuture(delayNano, intervalNano, runnable));
  }

  /**
   * Queues the task for execution in the after the delay time has expired.
   *
   * @param runnable The task to execute
   * @param delay The time before the task is executed
   * @param unit The TimeUnit that the delay is specified in
   * @return A Future object that can be monitored for execution state or to cancel the pending execution
   */
  public Future<Runnable> schedule(@NonNull @NotNull Runnable runnable, long delay, @NonNull @NotNull TimeUnit unit) {
    long nano = System.nanoTime() + unit.toNanos(delay);
    totalScheduled.increment();
    return addTask(nano, new ScheduledFuture(nano, runnable));
  }

  /**
   * The total tasks that have been scheduled to run
   *
   * @return count of tasks scheduled
   */
  public long getTotalScheduled() {
    return totalScheduled.sum();
  }

  /**
   * The total number of executed tasks, this may differ from the scheduled count due to cancelled tasks or repeat tasks
   *
   * @return count of tasks executed
   */
  public long getTotalExecuted() {
    return totalExecuted.sum();
  }

  /**
   * This is the current depth of tasks waiting to execute, this should always be a small number, else this could indicate an issue
   *
   * @return the size of the pending execution queue
   */
  public int getDepth() {
    return scheduledTasks.size();
  }

  /**
   * This class runs the queued tasks and will reschedule if they need to be, this is an internal class and is not exposed via the API
   */
  private class TaskExecutor implements Runnable {

    @Override
    public void run() {
      if (!scheduledTasks.isEmpty()) {
        totalExecuted.increment();
        long now = System.nanoTime() + (TICK_PERIOD / 4) * 1000000L;
        Map.Entry<Long, ScheduledFuture> entry = scheduledTasks.firstEntry();
        while (entry != null && entry.getKey() <= now) {
          scheduledTasks.remove(entry.getKey());
          executor.submit(entry.getValue());
          entry = scheduledTasks.firstEntry();
        }
      }
    }
  }

  private ScheduledFuture addTask(long time, ScheduledFuture task){
    ScheduledFuture collision = task;
    while(collision != null){
      collision = scheduledTasks.put(time, collision);
      if(collision != null) {
        time++;
      }
    }
    return task;
  }

  /**
   * This is the Future object returned when a task is queued for execution. It enables the cancellation and, hence, removal of the task from the pending queue
   */
  private class ScheduledFuture implements Future<Runnable>, Runnable {

    private final Runnable task;
    private final long delay;
    private final long interval;

    private volatile boolean cancelled;
    private volatile boolean done;

    public ScheduledFuture(long delay, Runnable task) {
      this(delay, -1, task);
    }

    public ScheduledFuture(long delay, long interval, Runnable task) {
      this.task = task;
      this.delay = delay;
      this.interval = interval;
      cancelled = false;
      done = false;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      if (!cancelled) {
        cancelled = true;
        return (scheduledTasks.remove(delay) != null);
      }
      return true;
    }

    @Override
    public boolean isCancelled() {
      return cancelled;
    }

    @Override
    public boolean isDone() {
      return done;
    }

    @Override
    public Runnable get() {
      return task;
    }

    @Override
    public Runnable get(long timeout, @NotNull TimeUnit unit) {
      return task;
    }

    public void run() {
      if (!cancelled) {
        done = true;
        task.run();
        if (interval != -1) {
          addTask(System.nanoTime()+interval, this);
          done = false;
        }
      }
    }
  }
}
