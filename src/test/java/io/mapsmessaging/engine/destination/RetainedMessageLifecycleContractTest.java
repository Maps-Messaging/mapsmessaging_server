/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.engine.destination;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RetainedMessageLifecycleContractTest {

  @TempDir
  Path directory;

  @Test
  void replacingAndClearingRetainedIdentifierReturnsPreviousOwner() throws Exception {
    RetainManager manager = new RetainManager(false, directory.toString());
    try {
      assertNull(manager.current());
      assertNull(manager.replace(10L));
      assertEquals(Long.valueOf(10L), manager.current());
      assertEquals(Long.valueOf(10L), manager.replace(20L));
      assertEquals(Long.valueOf(20L), manager.current());
      assertEquals(Long.valueOf(20L), manager.replace(null));
      assertNull(manager.current());
    } finally {
      manager.close();
    }
  }

  @Test
  void persistedRetainedIdentifierIsRestoredAcrossManagerRestart() throws Exception {
    RetainManager first = new RetainManager(true, directory.toString());
    try {
      first.replace(42L);
      assertEquals(Long.valueOf(42L), first.current());
    } finally {
      first.close();
    }

    RetainManager reopened = new RetainManager(true, directory.toString());
    try {
      assertEquals(Long.valueOf(42L), reopened.current());
      assertEquals(Long.valueOf(42L), reopened.replace(null));
      assertNull(reopened.current());
    } finally {
      reopened.close();
    }

    RetainManager cleared = new RetainManager(true, directory.toString());
    try {
      assertNull(cleared.current());
    } finally {
      cleared.close();
    }
  }
}
