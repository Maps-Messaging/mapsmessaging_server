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

package io.mapsmessaging.utilities.threads.tasks;

import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import lombok.NonNull;
import lombok.ToString;
import org.apache.logging.log4j.ThreadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;

/**
 * This class abstraction is a thread safe task scheduler that will manage the threading access of the task schedulers.
 *<br>
 *   It has no threads itself, rather, when a task is added to schedule, if it is the first task to be queued then this thread is used
 *   to execute the task and any additional tasks that have been queued while the first task was running. If this thread runs more than
 *   a configured number of tasks then it will off-load future tasks to a dedicated thread and unwind itself.
 *<br>
 *   This reduces the overall number of threads that are just waiting on queues and reduces the time for a task to be executed. The important
 *   aspect of this mechanism is that no locks are required to add or execute tasks and can be used to remove the standard
 * <code>
 *   synchronized(lock){
 *      // doSomething
 *   }
 * </code>
 *
 * This can change by using Task that can be queued. Since the queue is executed by a single thread, no locking is required and order is guaranteed.
 *
 * The <code>domain</code> field is used by tasks to ensure that the executing thread is meant to be running the code, offering the ability to ensure no code
 * by passes the task queue mechanism.
 *
 * @param <V> - is what will be returned by the future on completion of the task
 *
 *  @since 1.0
 *  @author Matthew Buckton
 *  @version 1.0
 */
@ToString
public abstract class ConcurrentTaskScheduler<V> implements TaskScheduler<V> {

  //Allow a maximum of so many tasks when the thread is external to the task scheduler
  protected static final int MAX_TASK_EXECUTION_EXTERNAL_THREAD = 10;
  //Allow a maximum of so many tasks in a single scheduled runner execution
  protected static final int MAX_TASK_EXECUTION_SCHEDULED_THREAD = Integer.MAX_VALUE;
  private static final String DOMAIN = "domain";

  private final ThreadStateContext context;

  protected final AtomicLong outstanding;
  protected final LongAdder offloadedCount;
  protected final LongAdder totalQueued;
  protected final Runnable offloadThread;

  protected volatile long maxOutstanding;
  protected volatile boolean shutdown;


  protected ConcurrentTaskScheduler(@NonNull @NotNull String domain) {
    context = new ThreadStateContext();
    context.add(DOMAIN, domain);
    context.add("TaskQueue", this);
    outstanding = new AtomicLong(0);
    maxOutstanding = 0;
    totalQueued = new LongAdder();
    offloadedCount = new LongAdder();
    offloadThread = new QueueRunner();
    shutdown = false;
  }


  @Override
  public void shutdown(boolean wait){
    shutdown = true;
    //
    // If a Future Task is closing this scheduler then we simply clear the queue and cancel any tasks
    // that may still be active
    String localDomain = (String) context.get(DOMAIN);
    ThreadStateContext threadStateContext = ThreadLocalContext.get();
    if(threadStateContext != null){
      String threadDomain = (String)threadStateContext.get(DOMAIN);
      if(localDomain != null && localDomain.equalsIgnoreCase(threadDomain)){
        // OK we are running a task that is closing this task scheduler, so we can clear out the queue
        FutureTask<V> task = poll();
        while (task != null) {
          task.cancel(true);
          task = poll();
        }
      }
    }
  }

  /**
   * @return the number of times this queue has had an offload thread take over
   */
  public long getOffloadCount(){
    return offloadedCount.sum();
  }

  /**
   * @return the maximum number of tasks that have been waiting
   */
  public long getMaxOutstanding(){
    return maxOutstanding;
  }

  /**
   * @return the total number of tasks that have been queued
   */
  public long getTotalTasksQueued(){
    return totalQueued.sum();
  }

  /**
   *
   * @return the current number of tasks waiting for execution
   */
  public long getOutstanding(){
    return outstanding.get();
  }

  /**
   * This function needs to be implemented in any class that extends this
   *
   * @return The next task to execute in this queue
   */
  protected abstract @Nullable FutureTask<V> poll();

  /**
   * Entry point to start processing tasks off the queue when adding
   */
  protected void executeQueue() {
    totalQueued.increment();
    long count = outstanding.incrementAndGet();
    if (count > maxOutstanding) {
      maxOutstanding = count;
    }
    //If we are equal to 1 we enter the queue execution path and process our task, this will lead to scheduling a the
    // QueueRunner if necessary
    if (count == 1) {
      internalExecuteQueue(MAX_TASK_EXECUTION_EXTERNAL_THREAD);
    }
  }

  private void internalExecuteQueue(int maxTaskExecutions) {
    Map<String, String> logContext = ThreadContext.getContext();
    ThreadStateContext originalDomain = ThreadLocalContext.get();
    ThreadLocalContext.set(context);
    try {
      taskRun(maxTaskExecutions);
    } finally {
      ThreadContext.putAll(logContext);
      if(originalDomain != null){
        ThreadLocalContext.set(originalDomain);
      }
      else{
        ThreadLocalContext.remove();
      }
    }
  }

  /**
   * Task Loop that runs until the queue is empty or offloaded to a dedicated thread
   */
  private void taskRun(int maxTaskExecutions) {
    int runnerCount = 0;
    FutureTask<V> task = poll();
    while (task != null) {
      task.run();
      runnerCount++;
      long count = outstanding.decrementAndGet();
      //If we return a value higher than zero we still have work to be done
      if (count != 0) {
        //If we have hit our max task executions schedule a new instance to run
        if (runnerCount >= maxTaskExecutions) {
          offloadedCount.increment();
          SimpleTaskScheduler.getInstance().submit(offloadThread);
          return;
        }
        //Otherwise keep processing
        task = poll();
      } else {
        //We have completed all of our work
        return;
      }
    }
  }
  /**
   * Class used for the offload thread
   */
  private class QueueRunner implements Runnable {

    final Map<String, String> context;

    public QueueRunner(){
      context = ThreadContext.getContext();
    }

    public void run(){
      String threadName = Thread.currentThread().getName();
      Thread.currentThread().setName("TaskQueue_OffLoad");
      ThreadContext.putAll(context); // Ensure the logging thread context is copied over
      internalExecuteQueue(MAX_TASK_EXECUTION_SCHEDULED_THREAD);
      Thread.currentThread().setName(threadName);
    }
  }
}