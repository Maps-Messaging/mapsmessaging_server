package io.mapsmessaging.dto.rest.config.protocol.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MavlinkConfigDTOTest {

  @Test
  void defaultConfigurationIsListenOnly() {
    MavlinkConfigDTO config = new MavlinkConfigDTO();

    assertEquals("mavlink", config.getType());
    assertNull(config.getSystemId());
    assertNull(config.getComponentId());
    assertFalse(config.hasLocalMavlinkIdentity());
    assertEquals(30, config.getHeartbeatIntervalSeconds());
    assertEquals("", config.getDialectName());
  }

  @Test
  void validPositiveSystemAndComponentIdsEnableLocalIdentity() {
    MavlinkConfigDTO config = new MavlinkConfigDTO();

    config.setSystemId(1);
    config.setComponentId(1);
    assertTrue(config.hasLocalMavlinkIdentity());

    config.setSystemId(255);
    config.setComponentId(255);
    assertTrue(config.hasLocalMavlinkIdentity());
  }

  @Test
  void missingZeroOrNegativeIdentityComponentsRemainListenOnly() {
    MavlinkConfigDTO config = new MavlinkConfigDTO();

    config.setSystemId(1);
    assertFalse(config.hasLocalMavlinkIdentity());

    config.setComponentId(1);
    config.setSystemId(0);
    assertFalse(config.hasLocalMavlinkIdentity());

    config.setSystemId(1);
    config.setComponentId(0);
    assertFalse(config.hasLocalMavlinkIdentity());

    config.setSystemId(-1);
    config.setComponentId(190);
    assertFalse(config.hasLocalMavlinkIdentity());
  }
}
