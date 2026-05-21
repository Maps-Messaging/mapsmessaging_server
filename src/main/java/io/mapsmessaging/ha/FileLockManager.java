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

package io.mapsmessaging.ha;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.utilities.GsonFactory;
import lombok.Getter;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class FileLockManager implements AutoCloseable {

  private static final long DEFAULT_RETRY_SLEEP_MILLIS = 1000;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final Path lockFilePath;
  private final Path heartbeatFilePath;
  private final Path stopSignalFilePath;
  private final long leaseTimeoutMillis;
  private final AtomicBoolean shutdown = new AtomicBoolean(false);
  private final LockInfo lockInfo;

  @Getter
  private volatile boolean locked = false;

  private Runnable onShutdownCallback;
  private FileChannel channel;
  private FileLock lock;
  private Thread heartbeatThread;
  private Thread watchThread;
  private final ExitHandler exitHandler;

  public FileLockManager(Path lockFilePath, long leaseTimeoutMillis) {
    this(lockFilePath, leaseTimeoutMillis, new SystemExitHandler());
  }

  public FileLockManager(Path lockFilePath, long leaseTimeoutMillis, ExitHandler exitHandler) {
    this.exitHandler = exitHandler;
    this.lockFilePath = lockFilePath.toAbsolutePath();
    this.heartbeatFilePath = Path.of(lockFilePath + ".heartbeat");
    this.stopSignalFilePath = Path.of(lockFilePath + ".stop");
    this.leaseTimeoutMillis = leaseTimeoutMillis;
    lockInfo = new LockInfo();
  }

  @SuppressWarnings("java:S2095")
  public boolean tryAcquireLockWithTakeover() {
    boolean acquired = false;
    try {
      channel = new RandomAccessFile(lockFilePath.toFile(), "rw").getChannel();

      while (!shutdown.get()) {
        FileLock attemptedLock = null;
        try {
          attemptedLock = channel.tryLock();
        } catch (OverlappingFileLockException ignored) {
          // Same JVM already holds an overlapping lock. Treat as "lock unavailable".
          attemptedLock = null;
        } catch (IOException ignored) {
          attemptedLock = null;
        }

        if (attemptedLock != null) {
          lock = attemptedLock;
          locked = true;
          acquired = true;

          Files.deleteIfExists(stopSignalFilePath);
          writeLockMetadata();
          startHeartbeat();
          startWatchService();
          return true;
        }

        if (isHeartbeatStale()) {
          logger.log(LOCKFILE_STALE_HEARTBEAT);
          try {
            Files.deleteIfExists(heartbeatFilePath);
          } catch (IOException ignored) {
            // ignore
          }
        }

        try {
          Thread.sleep(DEFAULT_RETRY_SLEEP_MILLIS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    } catch (IOException ignored) {
      // ignore
    } finally {
      if (!acquired) {
        // If we did not acquire, make sure we don't leak a handle.
        try {
          if (lock != null && lock.isValid()) {
            lock.release();
          }
        } catch (IOException ignored) {
        } finally {
          lock = null;
        }

        try {
          if (channel != null && channel.isOpen()) {
            channel.close();
          }
        } catch (IOException ignored) {
        } finally {
          channel = null;
        }

        locked = false;
      }
    }
    return false;
  }

  private void writeLockMetadata() {
    try {
      Files.writeString(lockFilePath, "" + ProcessHandle.current().pid(),
          StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException ignored) {
    }
  }

  private void startHeartbeat() {
    heartbeatThread = new Thread(() -> {
      while (!shutdown.get() && locked) {
        try {
          lockInfo.setLastHeartbeat(OffsetDateTime.now().toString());
          String json = GsonFactory.getInstance().getSimpleGson().toJson(lockInfo);
          Files.writeString(heartbeatFilePath, json);
        } catch (IOException ignored) {
        }

        try {
          Thread.sleep(leaseTimeoutMillis / 3);
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }, "FileLock-Heartbeat");
    heartbeatThread.setDaemon(true);
    heartbeatThread.start();
  }

  private void startWatchService() {
    watchThread = new Thread(this::runWatchLoop, "FileLock-WatchService");
    watchThread.setDaemon(true);
    watchThread.start();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        close();
      } catch (Exception ignored) {
        // ignore
      }
    }));
  }

  private void runWatchLoop() {
    try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
      Path dir = lockFilePath.getParent();
      if (dir == null) {
        return;
      }

      dir.register(watchService, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_CREATE);

      while (!shutdown.get()) {
        WatchKey key = watchService.take();
        processWatchEvents(key);
        if (!key.reset()) {
          break;
        }
      }
    } catch (IOException | InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @SuppressWarnings("unchecked")
  private void processWatchEvents(WatchKey key) {
    for (WatchEvent<?> event : key.pollEvents()) {
      Path name = ((WatchEvent<Path>) event).context();
      if (name == null) {
        continue;
      }

      if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE &&
          name.equals(lockFilePath.getFileName())) {
        handleLockFileDeleted();
      } else if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE &&
          name.equals(stopSignalFilePath.getFileName())) {
        handleStopSignalDetected();
      }
    }
  }

  private void handleLockFileDeleted() {
    logger.log(LOCKFILE_DELETED);
    shutdownAndExit();
  }

  private void handleStopSignalDetected() {
    logger.log(LOCKFILE_STOP_DETECTED);
    shutdownAndExit();
  }

  private void shutdownAndExit() {
    shutdown();
    if (onShutdownCallback != null) {
      onShutdownCallback.run();
    }
    exitHandler.accept(1);
  }

  private boolean isHeartbeatStale() {
    try {
      if (!Files.exists(heartbeatFilePath)) {
        return true;
      }
      String content = Files.readString(heartbeatFilePath).trim();
      LockInfo info = GsonFactory.getInstance().getSimpleGson().fromJson(content, LockInfo.class);
      OffsetDateTime lastBeat = OffsetDateTime.parse(info.getLastHeartbeat());
      return OffsetDateTime.now()
          .minus(leaseTimeoutMillis, java.time.temporal.ChronoUnit.MILLIS)
          .isAfter(lastBeat);
    } catch (IOException | RuntimeException e) {
      return true;
    }
  }

  public void setOnShutdown(Runnable onShutdownCallback) {
    this.onShutdownCallback = onShutdownCallback;
  }

  public boolean isShutdown() {
    return shutdown.get();
  }

  public void shutdown() {
    shutdown.set(true);
  }

  @Override
  public void close() {
    try {
      if (heartbeatThread != null) {
        heartbeatThread.interrupt();
        heartbeatThread = null;
      }
      if (watchThread != null) {
        watchThread.interrupt();
        watchThread = null;
      }
      if (lock != null && lock.isValid()) {
        lock.release();
      }
      if (channel != null && channel.isOpen()) {
        channel.close();
      }
      Files.deleteIfExists(heartbeatFilePath);
      Files.deleteIfExists(stopSignalFilePath);
    } catch (IOException ignored) {
    } finally {
      lock = null;
      channel = null;
      locked = false;
    }
  }
}
