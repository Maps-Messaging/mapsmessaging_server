/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.utilities.scheduler;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class SimpleTaskScheduler implements ScheduledExecutorService {

  private static final SimpleTaskScheduler instance = new SimpleTaskScheduler();

  public static SimpleTaskScheduler getInstance() {
    return instance;
  }


  private final ScheduledThreadPoolExecutor scheduler;

  private SimpleTaskScheduler() {
    scheduler = new ScheduledThreadPoolExecutor(10);
    scheduler.setRemoveOnCancelPolicy(true);
    scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
  }


  @NotNull
  @Override
  public ScheduledFuture<?> schedule(@NotNull Runnable command, long delay, @NotNull TimeUnit unit) {
    return scheduler.schedule(new InterceptedRunnable(command), delay, unit);
  }

  @NotNull
  @Override
  public <V> ScheduledFuture<V> schedule(@NotNull Callable<V> callable, long delay, @NotNull TimeUnit unit) {
    return scheduler.schedule(new InterceptedCallable<>(callable), delay, unit);
  }

  @NotNull
  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(@NotNull Runnable command, long initialDelay, long period, @NotNull TimeUnit unit) {
    return scheduler.scheduleAtFixedRate(command, initialDelay, period, unit);
  }

  @NotNull
  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(@NotNull Runnable command, long initialDelay, long period, @NotNull TimeUnit unit) {
    return scheduler.scheduleWithFixedDelay(new InterceptedRunnable(command), initialDelay, period, unit);
  }

  @Override
  public void shutdown() {
    scheduler.shutdown();
  }

  @NotNull
  @Override
  public List<Runnable> shutdownNow() {
    return scheduler.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return scheduler.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return scheduler.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
    return scheduler.awaitTermination(timeout, unit);
  }

  @NotNull
  @Override
  public <T> Future<T> submit(@NotNull Callable<T> task) {
    return scheduler.submit(task);
  }

  @NotNull
  @Override
  public <T> Future<T> submit(@NotNull Runnable task, T result) {
    return scheduler.submit(task, result);
  }

  @NotNull
  @Override
  public Future<?> submit(@NotNull Runnable task) {
    return scheduler.submit(task);
  }

  @NotNull
  @Override
  public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return scheduler.invokeAll(tasks);
  }

  @NotNull
  @Override
  public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException {
    return scheduler.invokeAll(tasks, timeout, unit);
  }

  @NotNull
  @Override
  public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return scheduler.invokeAny(tasks);
  }

  @Override
  public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return scheduler.invokeAny(tasks, timeout, unit);
  }

  @Override
  public void execute(@NotNull Runnable command) {
    scheduler.execute(new InterceptedRunnable(command));
  }

  public long getTotalScheduled() {
    return scheduler.getTaskCount();
  }

  public long getTotalExecuted() {
    return scheduler.getCompletedTaskCount();
  }

  public int getDepth() {
    return scheduler.getQueue().size();
  }

  public static final class InterceptedRunnable implements Runnable {

    private final Runnable runnable;

    public InterceptedRunnable(Runnable runnable) {
      this.runnable = runnable;
    }

    @Override
    public void run() {
      runnable.run();
    }
  }

  public static final class InterceptedCallable<T> implements Callable<T> {

    private final Callable<T> callable;

    public InterceptedCallable(Callable<T> callable) {
      this.callable = callable;
    }

    @Override
    public T call() throws Exception {
      return callable.call();
    }
  }

}
