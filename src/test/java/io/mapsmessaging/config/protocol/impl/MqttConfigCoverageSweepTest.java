package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MqttConfigCoverageSweepTest {
  @Test
  void mqttKeepAliveBoundsAreReadFromConfiguration() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("minServerKeepAlive", 12);
    properties.put("maxServerKeepAlive", 345);

    MqttConfig config = new MqttConfig(properties);

    assertEquals(12, config.getMinServerKeepAlive());
    assertEquals(345, config.getMaxServerKeepAlive());
    assertFalse(config.update(new BaseConfigDTO()));
    assertNotNull(config.toConfigurationProperties());
  }
}
