/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

/** CSD01 Topic Type 0/1/3 ownership and invalid alias rejection. */
class MqttSn2TopicAliasesTest {
  @Test
  void predefinedAndSessionAliasesAreDistinctNamespaces() throws Exception {
    MqttSn2TopicAliases aliases = new MqttSn2TopicAliases();
    assertEquals(1, aliases.register("session/topic"));
    assertEquals(1, aliases.register("session/topic"));
    assertEquals("session/topic", aliases.resolve(0, 1, null, id -> "predefined/topic"));
    assertEquals("predefined/topic", aliases.resolve(1, 1, null, id -> "predefined/topic"));
    assertEquals("inline/topic", aliases.resolve(3, 0, "inline/topic", id -> null));
    assertThrows(IOException.class, () -> aliases.resolve(2, 1, null, id -> null));
    assertThrows(IOException.class, () -> aliases.resolve(0, 200, null, id -> null));
  }

  @Test
  void newSessionCannotReuseOldConnectionAlias() throws Exception {
    MqttSn2TopicAliases aliases = new MqttSn2TopicAliases();
    aliases.register("previous/topic");
    aliases.clear();
    assertThrows(IOException.class, () -> aliases.resolve(0, 1, null, id -> null));
    assertEquals(1, aliases.register("new/topic"));
    assertEquals("new/topic", aliases.resolve(0, 1, null, id -> null));
  }

  @Test
  void emptyNameCannotBeRegisteredOrResolved() {
    MqttSn2TopicAliases aliases = new MqttSn2TopicAliases();
    assertThrows(IOException.class, () -> aliases.register(""));
    assertThrows(IOException.class, () -> aliases.resolve(3, 0, "", id -> null));
  }
}
