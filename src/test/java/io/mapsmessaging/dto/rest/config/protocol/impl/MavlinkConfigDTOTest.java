/*
 * Copyright [ 2020 - 2024 ] Matthew Buckton
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.dto.rest.config.protocol.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MavlinkConfigDTOTest {

  @Test
  void listenOnlyConfigurationHasNoLocalIdentity() {
    MavlinkConfigDTO config = new MavlinkConfigDTO();

    assertFalse(config.hasLocalMavlinkIdentity());
  }

  @Test
  void zeroComponentIdCannotEnableOutboundMavlink() {
    MavlinkConfigDTO config = new MavlinkConfigDTO();
    config.setSystemId(255);
    config.setComponentId(0);

    assertFalse(config.hasLocalMavlinkIdentity());
  }

  @Test
  void validSystemAndComponentIdsEnableOutboundMavlink() {
    MavlinkConfigDTO config = new MavlinkConfigDTO();
    config.setSystemId(255);
    config.setComponentId(190);

    assertTrue(config.hasLocalMavlinkIdentity());
  }
}
