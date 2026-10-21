/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.storage.alias;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32C;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TopicAliasSnapshotValidationTest {

  @ParameterizedTest
  @CsvSource({
      "0, 3, a, 2, b, Invalid alias entry",
      "11, 12, a, 2, b, Corrupt alias snapshot",
      "1, 3, a, 1, b, Duplicate alias or topic",
      "1, 3, a, 2, a, Duplicate alias or topic",
      "1, 2, a, 2, b, Invalid alias allocation state"
  })
  void rejects_crc_valid_invalid_entries_without_partial_restore(
      int firstId, int next, String firstTopic, int secondId, String secondTopic,
      String message) throws Exception {
    TopicAliasRegistry registry = new TopicAliasRegistry(10);
    registry.register("old");
    byte[] bytes = snapshot(firstId, next, firstTopic.getBytes(StandardCharsets.UTF_8), secondId, secondTopic);
    IOException failure = assertThrows(IOException.class, () -> registry.restore(bytes));
    assertEquals(message, failure.getMessage());
    assertEquals(1, registry.size());
    assertEquals("old", registry.topic(1));
    assertEquals(2, registry.register("next"));
  }

  @Test
  void rejects_invalid_utf8_after_valid_crc_without_changing_mappings() throws Exception {
    TopicAliasRegistry registry = new TopicAliasRegistry(10);
    registry.register("old");
    byte[] bytes = snapshot(1, 3, new byte[]{(byte) 0xc3}, 2, "valid");
    IOException failure = assertThrows(IOException.class, () -> registry.restore(bytes));
    assertEquals("Invalid alias topic UTF-8", failure.getMessage());
    assertEquals("old", registry.topic(1));
    assertEquals(1, registry.size());
  }

  @Test
  void restores_sparse_aliases_and_preserves_next_allocation() throws Exception {
    TopicAliasRegistry original = new TopicAliasRegistry(10);
    original.register(2, "two");
    original.register(7, "seven");
    TopicAliasRegistry restored = new TopicAliasRegistry(10);
    restored.restore(original.snapshot());
    assertEquals("two", restored.topic(2));
    assertEquals(7, restored.alias("seven"));
    assertEquals(8, restored.register("next"));
  }

  private static byte[] snapshot(int firstId, int next, byte[] firstTopic, int secondId, String secondTopic) {
    byte[] second = secondTopic.getBytes(StandardCharsets.UTF_8);
    ByteBuffer buffer = ByteBuffer.allocate(32 + firstTopic.length + second.length);
    buffer.putInt(0x54415231).putInt(1).putInt(next).putInt(2);
    buffer.putInt(firstId).putShort((short) firstTopic.length).put(firstTopic);
    buffer.putInt(secondId).putShort((short) second.length).put(second);
    CRC32C crc = new CRC32C();
    crc.update(buffer.array(), 0, buffer.position());
    buffer.putInt((int) crc.getValue());
    return buffer.array();
  }
}
