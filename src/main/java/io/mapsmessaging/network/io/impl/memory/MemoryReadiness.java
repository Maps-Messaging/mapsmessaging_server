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

package io.mapsmessaging.network.io.impl.memory;

import io.mapsmessaging.memory.MemoryConnection;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.Selector;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/**
 * A pipe bridges memory readiness to an existing server selector. The watcher never
 * executes protocol code. Only that selector drains the signal and calls the task.
 * State belongs to this monitor; callbacks run outside it to preserve task lock order.
 */
final class MemoryReadiness implements Closeable, Selectable {
  private final MemoryConnection connection;
  private final Selector selector;
  private final Consumer<Throwable> failureHandler;
  private final Pipe pipe;
  private SelectionKey pipeKey;
  private final MemoryKey key = new MemoryKey();
  private final Thread watcher;
  private final long pollNanos;
  private final ByteBuffer signal = ByteBuffer.allocate(1);
  private final ByteBuffer drain = ByteBuffer.allocate(1);
  private boolean open = true;
  private boolean signalled;
  private boolean dispatching;
  private int interests;
  private int ready;
  private Selectable task;
  private Throwable failure;
  private long progress;
  private long retryAfterNanos;

  MemoryReadiness(MemoryConnection connection, Selector selector, long pollMicros, Consumer<Throwable> failureHandler) throws IOException {
    if (pollMicros < 1 || pollMicros > 1_000_000) {
      throw new IllegalArgumentException("readinessPollMicros must be between 1 and 1000000");
    }
    this.connection = connection;
    this.selector = selector;
    this.failureHandler = failureHandler;
    pollNanos = TimeUnit.MICROSECONDS.toNanos(pollMicros);
    pipe = Pipe.open();
    try {
      pipe.source().configureBlocking(false);
      pipe.sink().configureBlocking(false);
      pipeKey = await(selector.register(pipe.source(), SelectionKey.OP_READ, this));
    } catch (IOException | RuntimeException exception) {
      try {
        pipe.source().close();
      } catch (IOException cleanup) {
        exception.addSuppressed(cleanup);
      }
      try {
        pipe.sink().close();
      } catch (IOException cleanup) {
        exception.addSuppressed(cleanup);
      }
      throw exception;
    }
    watcher = Thread.ofVirtual().name("memory-readiness").unstarted(this::watch);
  }

  synchronized void start() {
    if (open && watcher.getState() == Thread.State.NEW) {
      watcher.start();
    }
  }

  synchronized FutureTask<SelectionKey> register(int selection, Selectable runner) throws ClosedChannelException {
    if (!open || key.cancelled) {
      throw new ClosedChannelException();
    }
    if ((selection & ~(SelectionKey.OP_READ | SelectionKey.OP_WRITE)) != 0) {
      throw new IllegalArgumentException("Memory endpoints support only read/write interest");
    }
    interests = selection;
    task = runner;
    LockSupport.unpark(watcher);
    FutureTask<SelectionKey> result = new FutureTask<>(() -> {
      currentPipeKey();
      return key;
    });
    result.run();
    return result;
  }

  synchronized FutureTask<SelectionKey> deregister(int selection) throws ClosedChannelException {
    return register(selection < 0 ? 0 : interests & ~selection, task);
  }

  synchronized void madeProgress() {
    progress++;
    retryAfterNanos = 0;
  }

  private void watch() {
    long delay = Math.min(10_000, pollNanos);
    while (!Thread.currentThread().isInterrupted()) {
      boolean pending;
      synchronized (this) {
        if (!open) {
          return;
        }
        try {
          if (!signalled && !dispatching) {
            if (connection.isClosed()) {
              failure = new IOException("Memory peer closed");
            }
            if (failure != null || readyOps() != 0) {
              signal.clear();
              signal.put((byte) 1).flip();
              signalled = pipe.sink().write(signal) > 0;
            }
          }
        } catch (IOException | RuntimeException exception) {
          failure = exception;
          // Closing the sink also makes the source readable, delivering failure on the selector.
          try {
            pipe.sink().close();
          } catch (IOException cleanup) {
            exception.addSuppressed(cleanup);
          }
          selector.wakeup();
          return;
        }
        pending = signalled;
      }
      LockSupport.parkNanos(this, delay);
      delay = pending ? pollNanos : Math.min(pollNanos, delay * 2);
    }
  }

  private int readyOps() {
    // Protected transports can request another flush even while their underlying ring is full.
    if (retryAfterNanos != 0 && System.nanoTime() - retryAfterNanos < 0) {
      return 0;
    }
    int operations = 0;
    if (task != null) {
      if ((interests & SelectionKey.OP_READ) != 0 && connection.hasData()) {
        operations |= SelectionKey.OP_READ;
      }
      if ((interests & SelectionKey.OP_WRITE) != 0 && connection.canWrite()) {
        operations |= SelectionKey.OP_WRITE;
      }
    }
    return operations;
  }

  @Override
  public void selected(Selectable selectable, Selector selectedSelector, int selection) {
    Selectable selectedTask;
    Throwable selectedFailure;
    int operations;
    long beforeProgress;
    synchronized (this) {
      if (!open || dispatching) {
        return;
      }
      dispatching = true;
      beforeProgress = progress;
      try {
        drain.clear();
        pipe.source().read(drain);
        operations = failure == null ? readyOps() : 0;
      } catch (IOException | RuntimeException exception) {
        failure = exception;
        operations = 0;
      }
      ready = operations;
      selectedTask = task;
      selectedFailure = failure;
    }
    try {
      if (selectedFailure != null) {
        failureHandler.accept(selectedFailure);
      } else if (operations != 0 && selectedTask != null) {
        selectedTask.selected(selectedTask, selectedSelector, operations);
      }
    } catch (RuntimeException exception) {
      failureHandler.accept(exception);
    } finally {
      synchronized (this) {
        if (operations != 0 && progress == beforeProgress) {
          retryAfterNanos = System.nanoTime() + pollNanos;
        }
        signalled = false;
        dispatching = false;
      }
      LockSupport.unpark(watcher);
    }
  }

  @Override
  public void close() throws IOException {
    synchronized (this) {
      if (!open) {
        return;
      }
      open = false;
      interests = 0;
      task = null;
      pipeKey.cancel();
    }
    watcher.interrupt();
    selector.wakeup();
    IOException failure = null;
    try {
      pipe.source().close();
    } catch (IOException exception) {
      failure = exception;
    }
    try {
      pipe.sink().close();
    } catch (IOException exception) {
      if (failure == null) {
        failure = exception;
      } else {
        failure.addSuppressed(exception);
      }
    }
    if (failure != null) {
      throw failure;
    }
  }

  /** The server selector can migrate channels when it rebuilds its physical selector. */
  private SelectionKey currentPipeKey() throws IOException {
    if (!pipeKey.isValid()) {
      pipeKey = await(selector.register(pipe.source(), SelectionKey.OP_READ, this));
    }
    return pipeKey;
  }

  private static SelectionKey await(FutureTask<SelectionKey> future) throws IOException {
    try {
      return future.get();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted registering memory readiness", exception);
    } catch (ExecutionException exception) {
      throw new IOException("Failed registering memory readiness", exception.getCause());
    }
  }

  /** Logical interest key; its underlying pipe always selects OP_READ for notifications. */
  private final class MemoryKey extends SelectionKey {
    @Override
    public SelectableChannel channel() {
      return pipe.source();
    }

    @Override
    public java.nio.channels.Selector selector() {
      synchronized (MemoryReadiness.this) {
        requireValid();
        try {
          return currentPipeKey().selector();
        } catch (IOException exception) {
          CancelledKeyException cancelledKey = new CancelledKeyException();
          cancelledKey.initCause(exception);
          throw cancelledKey;
        }
      }
    }

    @Override
    public boolean isValid() {
      synchronized (MemoryReadiness.this) {
        return open && !cancelled && pipe.source().isOpen();
      }
    }

    @Override
    public void cancel() {
      synchronized (MemoryReadiness.this) {
        interests = 0;
        task = null;
        cancelled = true;
      }
    }

    private boolean cancelled;

    @Override
    public int interestOps() {
      synchronized (MemoryReadiness.this) {
        requireValid();
        return interests;
      }
    }

    @Override
    public SelectionKey interestOps(int operations) {
      synchronized (MemoryReadiness.this) {
        requireValid();
        try {
          register(operations, task);
        } catch (ClosedChannelException exception) {
          throw new CancelledKeyException();
        }
        return this;
      }
    }

    @Override
    public int readyOps() {
      synchronized (MemoryReadiness.this) {
        requireValid();
        return ready;
      }
    }

    private void requireValid() {
      if (cancelled || !isValid()) {
        throw new CancelledKeyException();
      }
    }
  }
}
