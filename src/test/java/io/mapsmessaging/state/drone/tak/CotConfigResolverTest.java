/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.drone.tak;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.state.config.CotAffiliation;
import io.mapsmessaging.state.config.CotConfigDTO;
import java.util.List;
import org.junit.jupiter.api.Test;

class CotConfigResolverTest {

  @Test
  void mostSpecificNamespaceWins() {
    CotConfigDTO root = config("4817/#", CotAffiliation.FRIENDLY);
    CotConfigDTO specific = config("4817/catl/external/+/+/+", CotAffiliation.HOSTILE);
    CotConfigResolver resolver = new CotConfigResolver(List.of(root, specific));

    assertSame(specific, resolver.resolve("4817/catl/external/json/node/MessageTypeEnum_NODE_STATUS"));
    assertSame(root, resolver.resolve("4817/catl/maps/json/node/MessageTypeEnum_NODE_STATUS"));
  }

  @Test
  void mqttStyleWildcardsAreSupported() {
    assertTrue(CotConfigResolver.matches("4817/catl/+/+/+/+", "4817/catl/maps/json/node/MessageTypeEnum_NODE_STATUS"));
    assertTrue(CotConfigResolver.matches("/4817/catl/#", "4817/catl/maps/json/node/MessageTypeEnum_NODE_STATUS"));
  }

  @Test
  void unmatchedNamespaceIsNotMapped() {
    CotConfigResolver resolver = new CotConfigResolver(List.of(config("4817/#", CotAffiliation.FRIENDLY)));

    assertNull(resolver.resolve("mavlink/1/status"));
    assertNull(resolver.resolve(null));
  }

  private CotConfigDTO config(String namespacePath, CotAffiliation affiliation) {
    CotConfigDTO config = new CotConfigDTO();
    config.setNamespacePath(namespacePath);
    config.setAffiliation(affiliation);
    return config;
  }
}
