/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.io.impl.serial.threads;


import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class SerialIoExecutors implements AutoCloseable {

  private static final int SHUTDOWN_WAIT_SECONDS = 2;

  private static final SerialIoExecutors INSTANCE = new SerialIoExecutors();

  private final ConcurrentMap<String, SerialIoPoolHandle> poolsByPortName;

  private SerialIoExecutors() {
    poolsByPortName = new ConcurrentHashMap<>();
  }

  public static SerialIoExecutors getInstance() {
    return INSTANCE;
  }

  public SerialIoPoolHandle acquire(String portName) {
    String canonicalPortName = canonicalizePortName(portName);

    return poolsByPortName.computeIfAbsent(canonicalPortName, key -> {
      ThreadFactory readThreadFactory = new SerialThreadFactory("SerialRead", key);
      ThreadFactory writeThreadFactory = new SerialThreadFactory("SerialWrite", key);

      ExecutorService readExecutor = Executors.newSingleThreadExecutor(readThreadFactory);
      ExecutorService writeExecutor = Executors.newSingleThreadExecutor(writeThreadFactory);

      return new SerialIoPoolHandle(key, readExecutor, writeExecutor);
    });
  }

  /**
   * In this simplified design, close is a no-op for individual handles.
   * We keep pools around for the lifetime of the process.
   */
  public void release(String portName) {
    // Intentionally empty by design.
  }

  @Override
  public void close() {
    for (SerialIoPoolHandle handle : poolsByPortName.values()) {
      shutdownExecutor(handle.getReadExecutor());
      shutdownExecutor(handle.getWriteExecutor());
    }
    poolsByPortName.clear();
  }

  private void shutdownExecutor(ExecutorService executorService) {
    executorService.shutdown();
    try {
      boolean terminated = executorService.awaitTermination(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS);
      if (!terminated) {
        executorService.shutdownNow();
      }
    } catch (InterruptedException interruptedException) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private String canonicalizePortName(String portName) {
    Objects.requireNonNull(portName, "portName must not be null");
    String trimmed = portName.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("portName must not be empty");
    }
    return trimmed;
  }

  private static final class SerialThreadFactory implements ThreadFactory {

    private final String prefix;
    private final String portName;
    private final AtomicInteger threadNumber;

    private SerialThreadFactory(String prefix, String portName) {
      this.prefix = prefix;
      this.portName = portName;
      this.threadNumber = new AtomicInteger(1);
    }

    @Override
    public Thread newThread(Runnable runnable) {
      String name = prefix + "-" + portName + "-" + threadNumber.getAndIncrement();
      Thread thread = new Thread(runnable, name);
      thread.setDaemon(true);
      return thread;
    }
  }
}