/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commonsclause
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

package io.mapsmessaging.network.protocol.impl.local;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class LoopbackEngineExecutor implements Executor {

  private static final int DEFAULT_POOL_SIZE = 4;
  private static final int DEFAULT_QUEUE_CAPACITY = 10_000;

  private static final LoopbackEngineExecutor INSTANCE =
      new LoopbackEngineExecutor(DEFAULT_POOL_SIZE, DEFAULT_QUEUE_CAPACITY, "loopback-engine", true);

  private final ThreadPoolExecutor executor;

  private final AtomicLong submitted = new AtomicLong();
  private final AtomicLong executed = new AtomicLong();
  private final AtomicLong rejected = new AtomicLong();
  private final AtomicLong failed = new AtomicLong();

  private final Object idleMonitor = new Object();
  private final AtomicLong inFlight = new AtomicLong();

  public static LoopbackEngineExecutor getInstance() {
    return INSTANCE;
  }

  public LoopbackEngineExecutor(int poolSize, int queueCapacity, String threadNamePrefix, boolean daemonThreads) {
    if (poolSize < 1) {
      throw new IllegalArgumentException("poolSize must be >= 1");
    }
    if (queueCapacity < 1) {
      throw new IllegalArgumentException("queueCapacity must be >= 1");
    }
    Objects.requireNonNull(threadNamePrefix, "threadNamePrefix");

    ThreadFactory threadFactory = new ThreadFactory() {
      private final AtomicLong counter = new AtomicLong();

      @Override
      public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName(threadNamePrefix + "-" + counter.incrementAndGet());
        thread.setDaemon(daemonThreads);
        return thread;
      }
    };

    RejectedExecutionHandler rejectionHandler = (runnable, threadPoolExecutor) -> {
      throw new RejectedExecutionException("Loopback executor queue is full");
    };

    this.executor =
        new ThreadPoolExecutor(
            poolSize,
            poolSize,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(queueCapacity),
            threadFactory,
            rejectionHandler
        );
  }

  @Override
  public void execute(Runnable runnable) {
    submit(runnable);
  }

  public boolean submit(Runnable runnable) {
    Objects.requireNonNull(runnable, "runnable");

    submitted.incrementAndGet();

    try {
      schedule(runnable);
      return true;
    } catch (RejectedExecutionException rejectedExecutionException) {
      rejected.incrementAndGet();
      return false;
    }
  }

  private void schedule(Runnable runnable) {
    inFlight.incrementAndGet();
    try {
      executor.execute(() -> {
        try {
          runnable.run();
        } catch (Throwable throwable) {
          failed.incrementAndGet();
          throw throwable;
        } finally {
          executed.incrementAndGet();
          long remaining = inFlight.decrementAndGet();
          if (remaining == 0) {
            synchronized (idleMonitor) {
              idleMonitor.notifyAll();
            }
          }
        }
      });
    } catch (RuntimeException runtimeException) {
      long remaining = inFlight.decrementAndGet();
      if (remaining == 0) {
        synchronized (idleMonitor) {
          idleMonitor.notifyAll();
        }
      }
      throw runtimeException;
    }
  }

  public Executor asExecutor() {
    return executor;
  }

  public long getSubmittedCount() {
    return submitted.get();
  }

  public long getExecutedCount() {
    return executed.get();
  }

  public long getRejectedCount() {
    return rejected.get();
  }

  public long getFailedCount() {
    return failed.get();
  }

  public int getQueueSize() {
    return executor.getQueue().size();
  }

  public boolean awaitIdle(long timeout, TimeUnit unit) throws InterruptedException {
    if (timeout < 0) {
      throw new IllegalArgumentException("timeout must be >= 0");
    }
    Objects.requireNonNull(unit, "unit");

    long deadlineNanos = System.nanoTime() + unit.toNanos(timeout);

    if (inFlight.get() == 0) {
      return true;
    }

    synchronized (idleMonitor) {
      while (inFlight.get() != 0) {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) {
          return false;
        }
        TimeUnit.NANOSECONDS.timedWait(idleMonitor, remainingNanos);
      }
      return true;
    }
  }

  public void shutdown() {
    executor.shutdown();
  }

  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return executor.awaitTermination(timeout, unit);
  }
}
