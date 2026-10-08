/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Properties;

/**
 * Crash-conscious sender and receiver monotonic counter storage for CSD01.
 * A distinct file must be assigned to each configured protection endpoint.
 *
 * <p>Updates are serialized with an OS file lock, written to a temporary file,
 * synced and atomically renamed before an authenticated packet is accepted or
 * its outbound counter is returned. An unavailable atomic rename fails closed.</p>
 */
public final class MqttSn2PersistentCounterStore implements
    MqttSn2ProtectionVerifier.ReplayStore, MqttSn2HmacProtectionSession.CounterSource {

  private static final String TX = "tx.counter";
  private static final long MAX = 0xFFFFFFFFL;
  private final Path stateFile;
  private final Path lockFile;

  public MqttSn2PersistentCounterStore(Path stateFile) {
    this.stateFile = Objects.requireNonNull(stateFile, "stateFile").toAbsolutePath().normalize();
    this.lockFile = this.stateFile.resolveSibling(this.stateFile.getFileName() + ".lock");
  }

  @Override
  public boolean accept(byte[] senderIdentifier, int scheme, long counter) throws IOException {
    if (senderIdentifier == null || senderIdentifier.length != 8
        || scheme < 0 || scheme > 255 || counter < 0 || counter > MAX) {
      throw new IOException("Invalid inbound protection replay counter");
    }
    String name = "rx." + HexFormat.of().formatHex(senderIdentifier) + "." + scheme;
    return update(properties -> {
      long previous = read(properties, name);
      if (counter <= previous) return new Result<>(false, false);
      properties.setProperty(name, Long.toUnsignedString(counter));
      return new Result<>(true, true);
    });
  }

  @Override
  public byte[] nextCounter() throws IOException {
    long counter = update(properties -> {
      long next = read(properties, TX) + 1;
      if (next > MAX) throw new IOException("Outbound protection counter exhausted");
      properties.setProperty(TX, Long.toUnsignedString(next));
      return new Result<>(next, true);
    });
    return ByteBuffer.allocate(4).putInt((int) counter).array();
  }

  private static long read(Properties properties, String name) throws IOException {
    String raw = properties.getProperty(name);
    if (raw == null) return 0;
    try {
      long counter = Long.parseUnsignedLong(raw);
      if (counter > MAX) throw new IOException("Protection counter out of range: " + name);
      return counter;
    } catch (NumberFormatException e) {
      throw new IOException("Corrupt protection counter state: " + name, e);
    }
  }

  private record Result<T>(T value, boolean changed) {}

  @FunctionalInterface
  private interface Update<T> {
    Result<T> apply(Properties properties) throws IOException;
  }

  private synchronized <T> T update(Update<T> action) throws IOException {
    Path parent = stateFile.getParent();
    if (parent == null) throw new IOException("Counter store requires a parent directory");
    Files.createDirectories(parent);
    try (FileChannel lockChannel = FileChannel.open(lockFile,
        StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
      FileLock lock = lockChannel.tryLock();
      if (lock == null) throw new IOException("Protection counter store is already in use");
      try (lock) {
        Properties properties = new Properties();
        if (Files.exists(stateFile)) {
          try (var input = Files.newInputStream(stateFile)) {
            properties.load(input);
          }
        }
        Result<T> result = action.apply(properties);
        if (result.changed()) persist(properties, parent);
        return result.value();
      }
    } catch (OverlappingFileLockException e) {
      throw new IOException("Protection counter store is already in use", e);
    }
  }

  private void persist(Properties properties, Path parent) throws IOException {
    Path temp = Files.createTempFile(parent, "mqtt-sn-2-counter-", ".tmp");
    try {
      try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE,
          StandardOpenOption.TRUNCATE_EXISTING)) {
        OutputStream output = Channels.newOutputStream(channel);
        properties.store(output, "MQTT-SN 2.0 protected-session counters");
        output.flush();
        channel.force(true);
      }
      try {
        Files.move(temp, stateFile, StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException error) {
        throw new IOException("Atomic protection counter persistence required", error);
      }
      try (FileChannel directory = FileChannel.open(parent, StandardOpenOption.READ)) {
        directory.force(true);
      } catch (IOException unsupportedDirectorySync) {
        // Windows does not expose directory handles via FileChannel. The
        // replacement file was force-synced before its atomic rename.
        if (!System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT)
            .contains("windows")) {
          throw unsupportedDirectorySync;
        }
      }
    } finally {
      Files.deleteIfExists(temp);
    }
  }
}
