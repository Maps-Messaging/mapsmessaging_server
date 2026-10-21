/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.alias;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.UUID;

/**
 * Session-owned alias snapshot file. The session manager is responsible for
 * deleting this file when its owning session is permanently removed.
 */
public final class FileAliasSnapshotPersistence implements TopicAliasRegistry.SnapshotPersistence {
  private final Path file;

  public FileAliasSnapshotPersistence(Path file) {
    this.file = Objects.requireNonNull(file).toAbsolutePath().normalize();
  }

  @Override
  public byte[] load() throws IOException {
    return Files.exists(file) ? Files.readAllBytes(file) : null;
  }

  @Override
  public void save(byte[] snapshot) throws IOException {
    Path parent = file.getParent();
    Files.createDirectories(parent);
    Path tmp = parent.resolve("." + file.getFileName() + "." + UUID.randomUUID() + ".tmp");
    try {
      Files.write(tmp, snapshot, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
      try (FileChannel channel = FileChannel.open(tmp, StandardOpenOption.WRITE)) {
        channel.force(true);
      }
      Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      forceDirectory(parent);
    } finally {
      Files.deleteIfExists(tmp);
    }
  }

  @Override
  public void delete() throws IOException {
    if (Files.deleteIfExists(file)) forceDirectory(file.getParent());
  }

  private static void forceDirectory(Path directory) throws IOException {
    try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
      channel.force(true);
    }
  }
}
