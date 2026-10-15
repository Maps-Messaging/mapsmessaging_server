/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class TopicAliasRegistryIntegrationTest {

  @Test
  void registeredIdsRemainIndependentForEveryStateEngine() {
    TopicAliasManager alice = new TopicAliasManager(null, 10);
    TopicAliasManager bob = new TopicAliasManager(null, 10);
    assertEquals(2, Short.toUnsignedInt(alice.getTopicAlias("alice/topic")));
    assertEquals(2, Short.toUnsignedInt(bob.getTopicAlias("bob/topic")));
    assertEquals("alice/topic", alice.getTopic(2));
    assertEquals("bob/topic", bob.getTopic(2));
    assertEquals(-1, alice.findTopicAlias("bob/topic"));
  }

  @Test
  void duplicateRegistrationPreservesIdAndClearResetsAllocation() {
    TopicAliasManager aliases = new TopicAliasManager(null, 2);
    short first = aliases.getTopicAlias("one");
    assertEquals(first, aliases.getTopicAlias("one"));
    assertEquals(3, Short.toUnsignedInt(aliases.getTopicAlias("two")));
    assertEquals(-1, aliases.getTopicAlias("three"));
    aliases.clear();
    assertNull(aliases.getTopic(2));
    assertEquals(2, Short.toUnsignedInt(aliases.getTopicAlias("new")));
  }
}
