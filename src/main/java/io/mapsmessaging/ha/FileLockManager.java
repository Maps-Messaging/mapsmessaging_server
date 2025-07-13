package io.mapsmessaging.ha;

import lombok.Getter;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileLockManager implements AutoCloseable {
  private final Path lockFilePath;
  private final Path heartbeatFilePath;
  private final long leaseTimeoutMillis;
  private final AtomicBoolean shutdown = new AtomicBoolean(false);

  @Getter
  private volatile boolean locked = false;
  private FileChannel channel;
  private FileLock lock;
  private Thread heartbeatThread;

  public FileLockManager(Path lockFilePath, long leaseTimeoutMillis) {
    this.lockFilePath = lockFilePath;
    this.heartbeatFilePath = Path.of(lockFilePath.toString() + ".heartbeat");
    this.leaseTimeoutMillis = leaseTimeoutMillis;
  }

  public boolean tryAcquireLockWithTakeover() {
    try {
      channel = new RandomAccessFile(lockFilePath.toFile(), "rw").getChannel();
      while (!shutdown.get()) {
        lock = channel.tryLock();
        if (lock != null) {
          locked = true;
          startHeartbeat();
          return true;
        }

        // Check if existing heartbeat is stale
        if (isHeartbeatStale()) {
          System.err.println("Heartbeat is stale, attempting forced takeover...");
          // Try deleting heartbeat before takeover
          try {
            Files.deleteIfExists(heartbeatFilePath);
          } catch (IOException ignored) {
          }
          Thread.sleep(1000);
        }

        Thread.sleep(1000);
      }
    } catch (IOException | InterruptedException e) {
      // Log or handle as needed
    }
    return false;
  }

  private void startHeartbeat() {
    heartbeatThread = new Thread(() -> {
      while (!shutdown.get() && locked) {
        try {
          Files.writeString(heartbeatFilePath, Long.toString(System.currentTimeMillis()));
        } catch (IOException ignored) {
        }
        try {
          Thread.sleep(leaseTimeoutMillis / 3); // update heartbeat frequently
        } catch (InterruptedException ignored) {
          break;
        }
      }
    }, "FileLock-Heartbeat");
    heartbeatThread.setDaemon(true);
    heartbeatThread.start();
  }

  private boolean isHeartbeatStale() {
    try {
      if (!Files.exists(heartbeatFilePath)) return true;
      String content = Files.readString(heartbeatFilePath).trim();
      long lastBeat = Long.parseLong(content);
      return (System.currentTimeMillis() - lastBeat) > leaseTimeoutMillis;
    } catch (IOException | NumberFormatException e) {
      return true;
    }
  }

  public void shutdown() {
    shutdown.set(true);
    close();
  }

  @Override
  public void close() {
    try {
      if (heartbeatThread != null) {
        heartbeatThread.interrupt();
        heartbeatThread = null;
      }
      if (lock != null && lock.isValid()) {
        lock.release();
      }
      if (channel != null && channel.isOpen()) {
        channel.close();
      }
      Files.deleteIfExists(heartbeatFilePath);
    } catch (IOException ignored) {
    } finally {
      locked = false;
    }
  }
}
