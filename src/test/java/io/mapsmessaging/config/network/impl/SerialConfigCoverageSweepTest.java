package io.mapsmessaging.config.network.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SerialConfigCoverageSweepTest {
  @Test
  void serialConfigUsesSerialTypeAndBuildsDeviceDefaults() {
    SerialConfig config = new SerialConfig(new ConfigurationProperties());

    assertEquals("serial", config.getType());
    assertNotNull(config.getSerialDevice());
    assertFalse(config.update(new BaseConfigDTO()));
    assertNotNull(config.toConfigurationProperties());
  }
}
