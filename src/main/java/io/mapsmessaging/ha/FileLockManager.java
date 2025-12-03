package io.mapsmessaging.ha;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.utilities.GsonFactory;
import lombok.Getter;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class FileLockManager implements AutoCloseable {
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

  public FileLockManager(Path lockFilePath, long leaseTimeoutMillis) {
    this.lockFilePath = lockFilePath.toAbsolutePath();
    this.heartbeatFilePath = Path.of(lockFilePath + ".heartbeat");
    this.stopSignalFilePath = Path.of(lockFilePath + ".stop");
    this.leaseTimeoutMillis = leaseTimeoutMillis;
    lockInfo = new LockInfo();
  }

  @SuppressWarnings("java:S2095")
  public boolean tryAcquireLockWithTakeover() {
    try {
      channel = new RandomAccessFile(lockFilePath.toFile(), "rw").getChannel();
      while (!shutdown.get()) {
        lock = channel.tryLock();
        if (lock != null) {
          locked = true;
          Files.deleteIfExists(stopSignalFilePath); // clean up stale stop file
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
            // Ignore
          }
        }
        Thread.sleep(1000);
      }
    } catch (IOException eox) {
      // ignore
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return false;
  }
  private void writeLockMetadata() {
    try {
      Files.writeString(lockFilePath,  ""+ProcessHandle.current().pid(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException ignored) {}
  }

  private void startHeartbeat() {
    heartbeatThread = new Thread(() -> {
      while (!shutdown.get() && locked) {
        try {
          lockInfo.setLastHeartbeat(OffsetDateTime.now().toString());
          String json = GsonFactory.getInstance().getSimpleGson().toJson(lockInfo);
          Files.writeString(heartbeatFilePath, json);
        } catch (IOException ignored) {}
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
      dir.register(watchService, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_CREATE);

      while (!shutdown.get()) {
        WatchKey key = watchService.take();
        processWatchEvents(key);
        if (!key.reset()) break;
      }
    } catch (IOException | InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @SuppressWarnings("unchecked")
  private void processWatchEvents(WatchKey key) {
    for (WatchEvent<?> event : key.pollEvents()) {
      Path name = ((WatchEvent<Path>) event).context();
      if (name == null) continue;

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
    System.exit(1);
  }

  private boolean isHeartbeatStale() {
    try {
      if (!Files.exists(heartbeatFilePath)) return true;
      String content = Files.readString(heartbeatFilePath).trim();
      LockInfo info = GsonFactory.getInstance().getSimpleGson().fromJson(content, LockInfo.class);
      OffsetDateTime lastBeat = OffsetDateTime.parse(info.getLastHeartbeat());
      return OffsetDateTime.now().minus(leaseTimeoutMillis, java.time.temporal.ChronoUnit.MILLIS).isAfter(lastBeat);
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
      locked = false;
    }
  }
}
