package io.mapsmessaging.config.protocol.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocalLoopConfigTest {

  @Test
  void constructorAlwaysIdentifiesLoopProtocolType() {
    LocalLoopConfig config = new LocalLoopConfig();

    assertEquals("loop", config.getType());

    config.setType("changed");
    assertEquals("changed", config.getType());

    LocalLoopConfig fresh = new LocalLoopConfig();
    assertEquals("loop", fresh.getType());
  }
}