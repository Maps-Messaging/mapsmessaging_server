/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.counter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

import io.mapsmessaging.storage.delivery.TargetDataTempDirFactory;
import java.nio.channels.FileChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.mockito.MockedStatic;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class CounterPublicationPlatformTest {

  @TempDir(factory = TargetDataTempDirFactory.class)
  Path root;

  @Test
  void windows_bulk_publication_preserves_counters_without_opening_directory_channels() throws Exception {
    String originalPlatform = System.getProperty("os.name");
    try (MockedStatic<FileChannel> channels = denyChannel(root)) {
      System.setProperty("os.name", "Windows 11");
      Path destination = root.resolve("bulk");
      IndexedCounterBulkLoader.initialize(destination, List.of(new IndexedCounterBulkLoader.Entry("rx", 17)));
      try (IndexedCounterStore reopened = new IndexedCounterStore(destination.resolve("counters"))) {
        assertEquals(17, reopened.highWaterMark("rx"));
      }
      channels.verify(() -> FileChannel.open(root, StandardOpenOption.READ), never());
    } finally {
      restorePlatform(originalPlatform);
    }
  }

  @Test
  void windows_manifest_publication_and_compaction_preserve_counters_across_restart() throws Exception {
    String originalPlatform = System.getProperty("os.name");
    Path destination = root.resolve("managed");
    try (MockedStatic<FileChannel> channels = denyChannel(destination)) {
      System.setProperty("os.name", "Windows 11");
      try (GenerationCounterStore store = new GenerationCounterStore(destination, CounterDurability.STRICT, 1, 0, 1.0)) {
        store.accept("rx", 41);
        assertEquals(new CounterStore.Range(1, 128), store.reserve("tx", 128));
        store.compact();
      }
      try (GenerationCounterStore reopened = new GenerationCounterStore(destination, CounterDurability.STRICT, 1, 0, 1.0)) {
        assertEquals(41, reopened.highWaterMark("rx"));
        assertFalse(reopened.accept("rx", 41));
        assertEquals(new CounterStore.Range(129, 130), reopened.reserve("tx", 2));
      }
      channels.verify(() -> FileChannel.open(destination, StandardOpenOption.READ), never());
    } finally {
      restorePlatform(originalPlatform);
    }
  }

  @Test
  void non_windows_directory_sync_failure_remains_visible() throws Exception {
    String originalPlatform = System.getProperty("os.name");
    try (MockedStatic<FileChannel> channels = denyChannel(root)) {
      System.setProperty("os.name", "Linux");
      assertThrows(AccessDeniedException.class, () -> IndexedCounterBulkLoader.initialize(root.resolve("denied"), List.of()));
      channels.verify(() -> FileChannel.open(root, StandardOpenOption.READ));
    } finally {
      restorePlatform(originalPlatform);
    }
  }

  @Test
  void windows_file_access_failure_remains_visible() throws Exception {
    String originalPlatform = System.getProperty("os.name");
    Path destination = root.resolve("denied");
    Path ownershipFile = destination.resolve("owner.lock");
    try (MockedStatic<FileChannel> channels = denyChannel(ownershipFile)) {
      System.setProperty("os.name", "Windows 11");
      assertThrows(AccessDeniedException.class, () -> new GenerationCounterStore(destination, CounterDurability.STRICT, 1, 0, 1.0));
      channels.verify(() -> FileChannel.open(ownershipFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE));
    } finally {
      restorePlatform(originalPlatform);
    }
  }

  private static MockedStatic<FileChannel> denyChannel(Path deniedPath) {
    // Emulate Windows directory denial at the IO boundary; regular file IO remains real.
    return mockStatic(FileChannel.class, invocation -> {
      if (invocation.getMethod().getName().equals("open") && deniedPath.equals(invocation.getArgument(0))) {
        throw new AccessDeniedException(deniedPath.toString());
      }
      return invocation.callRealMethod();
    });
  }

  private static void restorePlatform(String platform) {
    if (platform == null) {
      System.clearProperty("os.name");
    } else {
      System.setProperty("os.name", platform);
    }
  }
}
