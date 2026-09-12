/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CotConfigDtoIntegrationTest {

  @Test
  void cotMappingsRoundTripThroughAdapterConfiguration() {
    TwinManagerConfigDTO config = new TwinManagerConfigDTO();
    CotConfigDTO mapping = new CotConfigDTO();
    mapping.setNamespacePath("4817/catl/maps/#");
    mapping.setAffiliation(CotAffiliation.FRIENDLY);

    config.setCot(List.of(mapping));

    assertTrue(config.getAdapterConfig().containsKey("cot"));
    assertEquals(List.of(mapping), config.getCot());

    config.setCot(List.of());
    assertFalse(config.getAdapterConfig().containsKey("cot"));
    assertTrue(config.getCot().isEmpty());
  }
}
