/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.counter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import io.mapsmessaging.storage.delivery.TargetDataTempDirFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

/**
 * Deterministic faults at the generation publication boundary.
 * These are NOT physical power-cut or filesystem durability tests.
 */
class GenerationCounterCrashRecoveryTest {
  @TempDir(factory = TargetDataTempDirFactory.class)
  Path root;

  private Path managed() {
    return root.resolve("managed");
  }

  private GenerationCounterStore open() throws IOException {
    return new GenerationCounterStore(managed(), CounterDurability.STRICT, 1, 0, 1.0);
  }

  @Test
  void murphyFailureBeforeManifestMoveKeepsPreviouslyCommittedCounters() throws Exception {
    byte[] initialManifest;
    try (GenerationCounterStore store = open()) {
      assertTrue(store.accept("rx/client", 73));
      assertEquals(new CounterStore.Range(1, 256), store.reserve("tx", 256));
      initialManifest = Files.readAllBytes(managed().resolve("ACTIVE"));
      AtomicBoolean injected = new AtomicBoolean();
      try (MockedStatic<Files> files = mockStatic(Files.class, invocation -> {
        if (isManifestMove(invocation.getMethod().getName(), invocation.getArguments())) {
          injected.set(true);
          throw new IOException("Injected failure before ACTIVE manifest publication");
        }
        return invocation.callRealMethod();
      })) {
        assertThrows(IOException.class, store::compact);
      }
      assertTrue(injected.get(), "Fault injection was not reached");
      assertArrayEquals(initialManifest, Files.readAllBytes(managed().resolve("ACTIVE")));
      assertEquals(73, store.highWaterMark("rx/client"));
      assertFalse(store.accept("rx/client", 73));
      assertEquals(new CounterStore.Range(257, 258), store.reserve("tx", 2));
    }
    try (GenerationCounterStore reopened = open()) {
      assertEquals(73, reopened.highWaterMark("rx/client"));
      assertFalse(reopened.accept("rx/client", 73));
      assertEquals(new CounterStore.Range(259, 259), reopened.reserve("tx", 1));
    }
  }

  @Test
  void murphyFailureAfterManifestMoveRecoversPublishedGeneration() throws Exception {
    byte[] initialManifest;
    try (GenerationCounterStore store = open()) {
      assertTrue(store.accept("rx/client", 73));
      assertEquals(new CounterStore.Range(1, 256), store.reserve("tx", 256));
      initialManifest = Files.readAllBytes(managed().resolve("ACTIVE"));
      AtomicBoolean injected = new AtomicBoolean();
      try (MockedStatic<Files> files = mockStatic(Files.class, invocation -> {
        if (isManifestMove(invocation.getMethod().getName(), invocation.getArguments())) {
          Object result = invocation.callRealMethod();
          injected.set(true);
          throw new IOException("Injected failure after ACTIVE manifest publication");
        }
        return invocation.callRealMethod();
      })) {
        assertThrows(IOException.class, store::compact);
      }
      assertTrue(injected.get(), "Fault injection was not reached");
      if (java.util.Arrays.equals(initialManifest, Files.readAllBytes(managed().resolve("ACTIVE")))) {
        throw new AssertionError("Expected a new published generation");
      }
      assertEquals(73, store.highWaterMark("rx/client"));
      assertFalse(store.accept("rx/client", 73));
      assertEquals(new CounterStore.Range(257, 258), store.reserve("tx", 2));
    }
    try (GenerationCounterStore reopened = open()) {
      assertEquals(73, reopened.highWaterMark("rx/client"));
      assertFalse(reopened.accept("rx/client", 73));
      assertEquals(new CounterStore.Range(259, 259), reopened.reserve("tx", 1));
    }
  }

  private static boolean isManifestMove(String method, Object[] args) {
    return method.equals("move") && args.length >= 2
        && args[1] instanceof Path target
        && target.getFileName().toString().equals("ACTIVE");
  }
}
