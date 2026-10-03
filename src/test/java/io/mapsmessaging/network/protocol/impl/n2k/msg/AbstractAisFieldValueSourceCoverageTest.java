/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.n2k.msg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class AbstractAisFieldValueSourceCoverageTest {

  @Test
  void missingFieldsReturnNullAndHasIsFalse() {
    TestSource source = new TestSource();

    assertFalse(source.has("missing"));
    assertNull(source.getLong("missing"));
    assertNull(source.getDouble("missing"));
    assertNull(source.getString("missing"));
  }

  @Test
  void longDoubleAndStringValuesAreStoredIndependently() {
    TestSource source = new TestSource();
    source.longValue("same", 12L);
    source.doubleValue("same", 4.5);
    source.stringValue("same", "value");

    assertTrue(source.has("same"));
    assertEquals(12L, source.getLong("same"));
    assertEquals(4.5, source.getDouble("same"));
    assertEquals("value", source.getString("same"));
  }

  @Test
  void nullLongIsIgnored() {
    TestSource source = new TestSource();
    source.longValue("value", null);

    assertFalse(source.has("value"));
  }

  @Test
  void nullDoubleIsIgnored() {
    TestSource source = new TestSource();
    source.doubleValue("value", null);

    assertFalse(source.has("value"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "alpha", " ", "  text  "})
  void stringStorageFollowsBaseContract(String value) {
    TestSource source = new TestSource();
    source.stringValue("value", value);

    if (value.isEmpty()) {
      assertFalse(source.has("value"));
      assertNull(source.getString("value"));
    } else {
      assertTrue(source.has("value"));
      assertEquals(value, source.getString("value"));
    }
  }

  @Test
  void nullStringIsIgnored() {
    TestSource source = new TestSource();
    source.stringValue("value", null);

    assertFalse(source.has("value"));
  }

  @Test
  void differentKeysDoNotInterfere() {
    TestSource source = new TestSource();
    source.longValue("long", 1L);
    source.doubleValue("double", 2.0);
    source.stringValue("string", "three");

    assertTrue(source.has("long"));
    assertTrue(source.has("double"));
    assertTrue(source.has("string"));
    assertNull(source.getString("long"));
    assertNull(source.getLong("double"));
    assertNull(source.getDouble("string"));
  }

  private static final class TestSource extends AbstractAisFieldValueSource {
    void longValue(String key, Long value) {
      putLong(key, value);
    }

    void doubleValue(String key, Double value) {
      putDouble(key, value);
    }

    void stringValue(String key, String value) {
      putString(key, value);
    }
  }
}
