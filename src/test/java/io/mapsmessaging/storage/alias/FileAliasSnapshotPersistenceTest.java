/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.alias;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.mockito.MockedStatic;

class FileAliasSnapshotPersistenceTest {
  @TempDir Path root;

  private Path state() { return root.resolve("sessions/owner-uuid.aliases"); }

  private TopicAliasRegistry open() throws IOException {
    TopicAliasRegistry registry = new TopicAliasRegistry(128,
        TopicAliasRegistry.PersistenceMode.SESSION_PERSISTENT,
        new FileAliasSnapshotPersistence(state()));
    registry.load();
    return registry;
  }

  @Test
  void happyReconnectRestoresMappingsBySameSessionUuid() throws Exception {
    TopicAliasRegistry first = open();
    first.register("devices/temperature");
    TopicAliasRegistry restored = open();
    assertEquals("devices/temperature", restored.topic(1));
    assertEquals(1, restored.alias("devices/temperature"));
    assertEquals(2, restored.register("devices/humidity"));
  }

  @Test
  void happyPermanentDeletionRemovesSessionAliasFile() throws Exception {
    TopicAliasRegistry registry = open();
    registry.register("devices/temperature");
    assertTrue(Files.isRegularFile(state()));
    registry.destroy();
    assertFalse(Files.exists(state()));
    assertEquals(0, open().size());
  }

  @Test
  void happySessionResetClearsMappingsButRetainsSnapshot() throws Exception {
    TopicAliasRegistry registry = open();
    registry.register("devices/temperature");
    registry.clear();
    assertTrue(Files.isRegularFile(state()));
    assertNull(open().alias("devices/temperature"));
  }

  @Test
  void sadDamagedSnapshotRefusesSessionRecovery() throws Exception {
    TopicAliasRegistry registry = open();
    registry.register("devices/temperature");
    Files.write(state(), new byte[]{1, 2, 3});
    assertThrows(IOException.class, this::open);
  }

  @Test
  void murphySnapshotReplacementLeavesNoTemporaryFiles() throws Exception {
    TopicAliasRegistry registry = open();
    for (int i = 0; i < 30; i++) registry.register("topic/" + i);
    try (var files = Files.list(state().getParent())) {
      assertEquals(1, files.count(), "All temporary snapshots should have been published or deleted");
    }
    assertEquals(30, open().size());
  }

  @Test
  @ResourceLock(Resources.SYSTEM_PROPERTIES)
  void windows_directory_force_does_not_break_alias_registration_or_deletion() throws Exception {
    Path directory = state().toAbsolutePath().getParent();
    String originalOperatingSystem = System.getProperty("os.name");
    try (MockedStatic<FileChannel> channels = mockStatic(FileChannel.class, invocation -> {
      if (invocation.getMethod().getName().equals("open") && directory.equals(invocation.getArgument(0))) {
        throw new AccessDeniedException(directory.toString());
      }
      return invocation.callRealMethod();
    })) {
      try {
        System.setProperty("os.name", "Windows 11");
        TopicAliasRegistry registry = open();
        assertEquals(1, registry.register("devices/temperature"));
        assertEquals(2, registry.register("devices/humidity"));
        assertEquals("devices/temperature", registry.topic(1));
        TopicAliasRegistry restored = open();
        assertEquals("devices/humidity", restored.topic(2));
        restored.destroy();
        assertFalse(Files.exists(state()));
        assertEquals(0, open().size());
      } finally {
        System.setProperty("os.name", originalOperatingSystem);
      }
    }
  }

  @Test
  @ResourceLock(Resources.SYSTEM_PROPERTIES)
  void supported_platform_directory_failure_remains_an_error() throws Exception {
    Path directory = state().toAbsolutePath().getParent();
    String originalOperatingSystem = System.getProperty("os.name");
    try (MockedStatic<FileChannel> channels = mockStatic(FileChannel.class, invocation -> {
      if (invocation.getMethod().getName().equals("open") && directory.equals(invocation.getArgument(0))) {
        throw new AccessDeniedException(directory.toString());
      }
      return invocation.callRealMethod();
    })) {
      try {
        System.setProperty("os.name", "Linux");
        TopicAliasRegistry registry = open();

        assertThrows(AccessDeniedException.class, () -> registry.register("devices/temperature"));
        assertThrows(IOException.class, () -> registry.topic(1));
      } finally {
        System.setProperty("os.name", originalOperatingSystem);
      }
    }
  }
}
