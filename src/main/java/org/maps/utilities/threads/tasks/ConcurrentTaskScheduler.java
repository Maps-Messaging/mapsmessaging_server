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

package org.maps.utilities.threads.tasks;

import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.apache.logging.log4j.ThreadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maps.utilities.threads.SimpleTaskScheduler;

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
public abstract class ConcurrentTaskScheduler<V> implements TaskScheduler<V> {

  private static final String DOMAIN = "domain";

  private final ThreadStateContext context;

  protected final AtomicLong outstanding;
  protected final AtomicLong maxOutstanding;
  protected final LongAdder offloadedCount;
  protected final LongAdder totalQueued;
  protected final AtomicBoolean offloaded;
  protected final Runnable offloadThread;

  protected volatile boolean shutdown;


  public ConcurrentTaskScheduler(@NotNull String domain) {
    context = new ThreadStateContext();
    context.add(DOMAIN, domain);
    context.add("TaskQueue", this);
    outstanding = new AtomicLong(0);
    maxOutstanding = new AtomicLong(0);
    totalQueued = new LongAdder();
    offloadedCount = new LongAdder();
    offloaded = new AtomicBoolean(false);
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
    return maxOutstanding.get();
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
   * Task Loop that runs until the queue is empty, or, offloaded to a dedicated thread
   */
  protected void taskRun() {
    int runnerCount = 0;
    FutureTask<V> task = poll();
    while (task != null) {
      task.run();
      runnerCount++;
      long count = outstanding.decrementAndGet();
      if (count > maxOutstanding.get()) {
        maxOutstanding.set(count);
      }
      if ((runnerCount > 10 || count > 10) && !offloaded.get()) {
        offloaded.set(true);
        offloadedCount.increment();
        SimpleTaskScheduler.getInstance().submit(offloadThread);
        return;
      }
      if (count != 0) {
        task = poll();
      } else {
        task = null;
        offloaded.set(false);
      }
    }
  }

  /**
   * Entry point to start processing tasks off the queue
   */
  protected void executeQueue() {
    Map<String, String> logContext = ThreadContext.getContext();
    ThreadStateContext originalDomain = ThreadLocalContext.get();
    ThreadLocalContext.set(context);
    try {
      taskRun();
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
   * Class used for the offload thread
   */
  class QueueRunner implements Runnable {
    Map<String, String> context;

    public QueueRunner(){
      context = ThreadContext.getContext();
    }

    public void run(){
      String threadName = Thread.currentThread().getName();
      Thread.currentThread().setName("TaskQueue_OffLoad");
      ThreadContext.putAll(context); // Ensure the logging thread context is copied over
      executeQueue();
      Thread.currentThread().setName(threadName);
    }
  }
}