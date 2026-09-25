/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
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

package io.mapsmessaging.engine.session;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class SessionExpiryTask implements Future<Void> {

  private final ExecutorService pipelineExecutor;
  private final Runnable cleanup;
  private final CompletableFuture<Void> completion;

  private Future<?> timerFuture;
  private Future<?> cleanupFuture;

  static SessionExpiryTask schedule(SessionExpiryScheduler scheduler, ExecutorService pipelineExecutor, Runnable cleanup,
      long delay, TimeUnit unit) {
    SessionExpiryTask task = new SessionExpiryTask(pipelineExecutor, cleanup);
    task.timerFuture = scheduler.schedule(task::timerFired, delay, unit);
    return task;
  }

  private SessionExpiryTask(ExecutorService pipelineExecutor, Runnable cleanup) {
    this.pipelineExecutor = pipelineExecutor;
    this.cleanup = cleanup;
    completion = new CompletableFuture<>();
  }

  private synchronized void timerFired() {
    if (completion.isDone()) {
      return;
    }
    try {
      cleanupFuture = pipelineExecutor.submit(() -> {
        try {
          if (!completion.isCancelled()) {
            cleanup.run();
          }
          completion.complete(null);
        } catch (Throwable throwable) {
          completion.completeExceptionally(throwable);
        }
      });
      if (completion.isCancelled()) {
        cleanupFuture.cancel(false);
      }
    } catch (RuntimeException runtimeException) {
      completion.completeExceptionally(runtimeException);
    }
  }

  @Override
  public synchronized boolean cancel(boolean mayInterruptIfRunning) {
    if (completion.isDone()) {
      return false;
    }
    if (timerFuture != null) {
      timerFuture.cancel(mayInterruptIfRunning);
    }
    if (cleanupFuture != null) {
      cleanupFuture.cancel(mayInterruptIfRunning);
    }
    return completion.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return completion.isCancelled();
  }

  @Override
  public boolean isDone() {
    return completion.isDone();
  }

  @Override
  public Void get() throws InterruptedException, ExecutionException {
    return completion.get();
  }

  @Override
  public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return completion.get(timeout, unit);
  }
}
