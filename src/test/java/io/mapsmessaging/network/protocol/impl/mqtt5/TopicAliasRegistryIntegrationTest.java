/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt5;

import static org.junit.jupiter.api.Assertions.*;

import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.TopicAlias;
import org.junit.jupiter.api.Test;

class TopicAliasRegistryIntegrationTest {

  @Test
  void mqtt5MappingsRemainConnectionLocal() {
    TopicAliasMapping inbound = new TopicAliasMapping("in");
    TopicAliasMapping outbound = new TopicAliasMapping("out");
    inbound.setMaximum(8);
    outbound.setMaximum(8);
    assertTrue(inbound.add("a", new TopicAlias(3)));
    assertNull(outbound.find(3));
    assertEquals("a", inbound.find(3));
    inbound.clearAll();
    assertNull(inbound.find(3));
  }

  @Test
  void mqtt5ReassignsAliasAndDropsStaleReverseLookup() {
    TopicAliasMapping mapping = new TopicAliasMapping("in");
    mapping.setMaximum(8);
    assertTrue(mapping.add("first", new TopicAlias(1)));
    assertTrue(mapping.add("second", new TopicAlias(1)));
    assertNull(mapping.find("first"));
    assertEquals("second", mapping.find(1));
    assertEquals(1, mapping.size());
  }

  @Test
  void mqtt5RejectsZeroAndAboveNegotiatedMaximum() {
    TopicAliasMapping mapping = new TopicAliasMapping("in");
    mapping.setMaximum(4);
    assertFalse(mapping.add("zero", new TopicAlias(0)));
    assertFalse(mapping.add("too-large", new TopicAlias(5)));
    assertTrue(mapping.add("valid", new TopicAlias(4)));
    assertEquals("valid", mapping.find(4));
  }

  @Test
  void mqtt5ZeroMaximumMeansNoAliases() {
    TopicAliasMapping mapping = new TopicAliasMapping("in");
    mapping.setMaximum(0);
    assertNull(mapping.create("topic"));
    assertFalse(mapping.add("topic", new TopicAlias(1)));
  }
}
