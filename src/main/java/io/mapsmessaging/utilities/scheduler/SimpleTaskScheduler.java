package io.mapsmessaging.utilities.scheduler;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jetbrains.annotations.NotNull;

public class SimpleTaskScheduler implements ScheduledExecutorService {

  private static final SimpleTaskScheduler instance = new SimpleTaskScheduler();

  public static SimpleTaskScheduler getInstance(){
    return instance;
  }


  private final ScheduledThreadPoolExecutor scheduler;

  private SimpleTaskScheduler(){
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

  public static final class InterceptedRunnable implements Runnable{

    private final Runnable runnable;

    public InterceptedRunnable(Runnable runnable){
      this.runnable = runnable;
    }
    @Override
    public void run() {
      runnable.run();
    }
  }

  public static final class InterceptedCallable<T> implements Callable<T>{

    private final Callable<T> callable;

    public InterceptedCallable(Callable<T> callable){
      this.callable = callable;
    }
    @Override
    public T call() throws Exception {
      return callable.call();
    }
  }

}
