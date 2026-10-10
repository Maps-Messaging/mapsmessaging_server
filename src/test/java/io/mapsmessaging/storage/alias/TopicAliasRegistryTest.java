/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.alias;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class TopicAliasRegistryTest {
  private static final class MemoryPersistence implements TopicAliasRegistry.SnapshotPersistence {
    byte[] bytes;
    int saves;
    int deletes;
    boolean failSave;
    boolean failDelete;
    @Override public byte[] load() { return bytes == null ? null : bytes.clone(); }
    @Override public void save(byte[] data) throws IOException {
      if (failSave) throw new IOException("Disk full");
      bytes = data.clone();
      saves++;
    }
    @Override public void delete() throws IOException {
      if (failDelete) throw new IOException("Delete failed");
      bytes = null;
      deletes++;
    }
  }

  private static TopicAliasRegistry persistent(MemoryPersistence backend) throws IOException {
    TopicAliasRegistry registry = new TopicAliasRegistry(100,
        TopicAliasRegistry.PersistenceMode.SESSION_PERSISTENT, backend);
    registry.load();
    return registry;
  }

  @Test void memoryOnlyDoesNotNeedOrTouchStorage() throws Exception {
    TopicAliasRegistry a = new TopicAliasRegistry(100);
    TopicAliasRegistry b = new TopicAliasRegistry(100);
    assertEquals(1, a.register("alpha"));
    assertEquals(1, b.register("beta"));
    assertEquals("alpha", a.topic(1));
    assertEquals("beta", b.topic(1));
    assertEquals(1, a.alias("alpha"));
    a.destroy();
    assertThrows(IOException.class, () -> a.register("next"));
    assertEquals("beta", b.topic(1));
  }

  @Test void persistentSessionRestoresAndClearsDurably() throws Exception {
    MemoryPersistence storage = new MemoryPersistence();
    TopicAliasRegistry first = persistent(storage);
    assertEquals(1, first.register("one"));
    assertEquals(2, first.register("two"));
    assertEquals(2, storage.saves);
    TopicAliasRegistry restarted = persistent(storage);
    assertEquals("one", restarted.topic(1));
    assertEquals(2, restarted.alias("two"));
    restarted.clear();
    assertEquals(0, persistent(storage).size());
    assertEquals(1, persistent(storage).register("three"));
  }

  @Test void sessionExpiryRemovesSnapshotButDisconnectDoesNot() throws Exception {
    MemoryPersistence storage = new MemoryPersistence();
    TopicAliasRegistry session = persistent(storage);
    session.register("one");
    assertNotNull(storage.bytes);
    assertEquals("one", persistent(storage).topic(1), "Disconnect must not delete state");
    session.destroy();
    assertNull(storage.bytes);
    assertEquals(1, storage.deletes);
    assertEquals(0, persistent(storage).size());
    assertThrows(IOException.class, () -> session.topic(1));
  }

  @Test void failingSavePoisonsRegistryWithoutExposingUncommittedAlias() throws Exception {
    MemoryPersistence storage = new MemoryPersistence();
    TopicAliasRegistry session = persistent(storage);
    storage.failSave = true;
    assertThrows(IOException.class, () -> session.register("not-committed"));
    assertThrows(IOException.class, () -> session.topic(1));
    storage.failSave = false;
    assertEquals(0, persistent(storage).size());
  }

  @Test void failingDeleteDoesNotPretendCleanupSucceeded() throws Exception {
    MemoryPersistence storage = new MemoryPersistence();
    TopicAliasRegistry session = persistent(storage);
    session.register("one");
    storage.failDelete = true;
    assertThrows(IOException.class, session::destroy);
    assertThrows(IOException.class, () -> session.topic(1));
    assertNotNull(storage.bytes);
  }

  @Test void invalidMappingsAndRangeAreRejected() throws Exception {
    TopicAliasRegistry registry = new TopicAliasRegistry(2);
    assertThrows(IOException.class, () -> registry.register(""));
    assertThrows(IOException.class, () -> registry.register(0, "a"));
    registry.register(1, "a");
    assertThrows(IOException.class, () -> registry.register(1, "b"));
    assertThrows(IOException.class, () -> registry.register(2, "a"));
    assertEquals(2, registry.register("b"));
    assertThrows(IOException.class, () -> registry.register("c"));
  }

  @Test void mqtt5RebindOnlyInMemoryAndBidirectionalMapsStayConsistent() throws Exception {
    TopicAliasRegistry registry = new TopicAliasRegistry(10);
    registry.rebind(1, "one");
    registry.rebind(1, "two");
    assertNull(registry.alias("one"));
    assertEquals("two", registry.topic(1));
    registry.rebind(3, "two");
    assertNull(registry.topic(1));
    assertEquals(3, registry.alias("two"));
    MemoryPersistence storage = new MemoryPersistence();
    assertThrows(IOException.class, () -> persistent(storage).rebind(1, "different"));
  }

  @Test void corruptSnapshotFailsWithoutPartialMappings() throws Exception {
    TopicAliasRegistry original = new TopicAliasRegistry(10);
    original.register("alpha");
    byte[] damaged = original.snapshot();
    damaged[12] ^= 0x40;
    TopicAliasRegistry restored = new TopicAliasRegistry(10);
    restored.register("old");
    assertThrows(IOException.class, () -> restored.restore(damaged));
    assertEquals("old", restored.topic(1));
  }

  @Test void rejectsCorruptPersistedSnapshotAndFailsClosed() throws Exception {
    MemoryPersistence storage = new MemoryPersistence();
    storage.bytes = new byte[]{1, 2, 3};
    TopicAliasRegistry target = new TopicAliasRegistry(10,
        TopicAliasRegistry.PersistenceMode.SESSION_PERSISTENT, storage);
    assertThrows(IOException.class, target::load);
    assertThrows(IOException.class, () -> target.register("not-safe"));
  }
}
