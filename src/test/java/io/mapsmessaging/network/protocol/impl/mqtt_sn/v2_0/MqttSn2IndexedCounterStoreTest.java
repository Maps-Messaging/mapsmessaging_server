/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MqttSn2IndexedCounterStoreTest {
  @TempDir Path directory;

  private Path storePath() {
    return directory.resolve("mqtt-sn-protection");
  }

  private static long unsignedCounter(byte[] wire) {
    return Integer.toUnsignedLong(ByteBuffer.wrap(wire).getInt());
  }

  @Test
  void happyInboundReplayIdentityAndSchemeSurviveRestart() throws Exception {
    byte[] senderA = {1, 2, 3, 4, 5, 6, 7, 8};
    byte[] senderB = {8, 7, 6, 5, 4, 3, 2, 1};
    try (MqttSn2IndexedCounterStore store = new MqttSn2IndexedCounterStore(storePath())) {
      assertTrue(store.accept(senderA, 0, 14));
      assertFalse(store.accept(senderA, 0, 14));
      assertFalse(store.accept(senderA, 0, 13));
      assertTrue(store.accept(senderA, 1, 1));
      assertTrue(store.accept(senderB, 0, 2));
    }
    try (MqttSn2IndexedCounterStore store = new MqttSn2IndexedCounterStore(storePath())) {
      assertFalse(store.accept(senderA, 0, 14));
      assertTrue(store.accept(senderA, 0, 15));
      assertFalse(store.accept(senderA, 1, 1));
      assertTrue(store.accept(senderB, 0, 3));
    }
  }

  @Test
  void happyOutboundNeverReissuesAfterRestart() throws Exception {
    try (MqttSn2IndexedCounterStore store = new MqttSn2IndexedCounterStore(storePath())) {
      assertEquals(1L, unsignedCounter(store.nextCounter()));
      assertEquals(2L, unsignedCounter(store.nextCounter()));
    }
    try (MqttSn2IndexedCounterStore store = new MqttSn2IndexedCounterStore(storePath())) {
      assertEquals(1025L, unsignedCounter(store.nextCounter()), "Unused reserved counters must not be reused");
    }
  }

  @Test
  void happyRangeRolloverIsContiguousWhileRunning() throws Exception {
    try (MqttSn2IndexedCounterStore store = new MqttSn2IndexedCounterStore(storePath())) {
      for (int i = 1; i <= 2050; i++) {
        assertEquals(i, unsignedCounter(store.nextCounter()), "Counter gap during live range rollover");
      }
    }
    try (MqttSn2IndexedCounterStore restarted = new MqttSn2IndexedCounterStore(storePath())) {
      assertEquals(3073L, unsignedCounter(restarted.nextCounter()),
          "Restart skips remaining reserved range after two rollovers");
    }
  }

  @Test
  void sadInvalidSenderAndCounterFailClosed() throws Exception {
    byte[] sender = {1, 2, 3, 4, 5, 6, 7, 8};
    try (MqttSn2IndexedCounterStore store = new MqttSn2IndexedCounterStore(storePath())) {
      assertThrows(IOException.class, () -> store.accept(null, 0, 1));
      assertThrows(IOException.class, () -> store.accept(new byte[7], 0, 1));
      assertThrows(IOException.class, () -> store.accept(sender, -1, 1));
      assertThrows(IOException.class, () -> store.accept(sender, 256, 1));
      assertThrows(IOException.class, () -> store.accept(sender, 0, 0));
      assertThrows(IOException.class, () -> store.accept(sender, 0, 0x1_0000_0000L));
    }
  }

  @Test
  void murphySecondWriterCannotShareCounterState() throws Exception {
    try (MqttSn2IndexedCounterStore first = new MqttSn2IndexedCounterStore(storePath())) {
      assertThrows(IOException.class, () -> new MqttSn2IndexedCounterStore(storePath()));
      assertEquals(1L, unsignedCounter(first.nextCounter()));
    }
  }

  @Test
  void murphyCorruptManifestFailsClosed() throws Exception {
    try (MqttSn2IndexedCounterStore store = new MqttSn2IndexedCounterStore(storePath())) {
      assertEquals(1L, unsignedCounter(store.nextCounter()));
    }
    java.nio.file.Files.write(storePath().resolve("ACTIVE"), new byte[]{1, 2, 3});
    assertThrows(IOException.class, () -> new MqttSn2IndexedCounterStore(storePath()));
  }
}
