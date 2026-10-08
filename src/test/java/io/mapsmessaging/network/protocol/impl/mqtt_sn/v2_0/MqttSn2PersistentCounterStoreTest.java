/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** CSD01 replay rejection and counter durability across gateway restart. */
class MqttSn2PersistentCounterStoreTest {
  @TempDir Path temp;

  @Test
  void countersSurviveReopeningWithoutAcceptingReplay() throws Exception {
    Path file = temp.resolve("protected-counters.properties");
    byte[] sender = {0,0,0,0,0,0,0,9};
    MqttSn2PersistentCounterStore first = new MqttSn2PersistentCounterStore(file);
    assertEquals(1, unsigned(first.nextCounter()));
    assertTrue(first.accept(sender, 0, 8));
    assertFalse(first.accept(sender, 0, 8));

    MqttSn2PersistentCounterStore restarted = new MqttSn2PersistentCounterStore(file);
    assertEquals(2, unsigned(restarted.nextCounter()));
    assertFalse(restarted.accept(sender, 0, 7));
    assertFalse(restarted.accept(sender, 0, 8));
    assertTrue(restarted.accept(sender, 0, 9));
    assertTrue(restarted.accept(sender, 1, 1), "Replay space is per sender and scheme");
  }

  @Test
  void corruptPersistenceCannotSilentlyRestartCounters() throws Exception {
    Path file = temp.resolve("corrupt.properties");
    Files.writeString(file, "tx.counter=invalid\n");
    MqttSn2PersistentCounterStore store = new MqttSn2PersistentCounterStore(file);
    assertThrows(IOException.class, store::nextCounter);
  }

  private static long unsigned(byte[] count) {
    return Integer.toUnsignedLong(ByteBuffer.wrap(count).getInt());
  }
}
