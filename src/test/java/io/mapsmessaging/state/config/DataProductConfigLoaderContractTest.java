/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.state.config;

import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.configuration.ConfigurationProperties;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataProductConfigLoaderContractTest {

  @Test
  void manager_returns_nonnull_properties_for_an_unknown_key() {
    assertNotNull(ConfigurationManager.getInstance().getProperties("absent-data-product-test"));
  }

  @Test
  void empty_twin_manager_configuration_returns_no_products() {
    ConfigurationManager manager = mock(ConfigurationManager.class);
    when(manager.getProperties("TwinManager")).thenReturn(new ConfigurationProperties());
    try (MockedStatic<ConfigurationManager> singleton = mockStatic(ConfigurationManager.class)) {
      singleton.when(ConfigurationManager::getInstance).thenReturn(manager);
      assertTrue(DataProductConfigLoader.load("absent-data-product-test", null).isEmpty());
    }
  }
}
