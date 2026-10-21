package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StompConfigCoverageSweepTest {
  @Test
  void stompSpecificBufferAndHeartbeatSettingsRoundTrip() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("maximumBufferSize", 4096);
    properties.put("maximumReceive", 32);
    properties.put("base64EncodeBinary", true);
    properties.put("heartbeatCanSendMillis", 1000);
    properties.put("heartbeatWantsReceiveMillis", 2000);
    properties.put("heartbeatToleranceMillis", 300);

    StompConfig config = new StompConfig(properties);

    assertEquals("stomp", config.getType());
    assertEquals(4096, config.getMaxBufferSize());
    assertEquals(32, config.getMaxReceive());
    assertTrue(config.isBase64EncodeBinary());
    assertFalse(config.update(new BaseConfigDTO()));
    assertEquals(
        300,
        config.toConfigurationProperties().getIntProperty("heartbeatToleranceMillis", -1));
  }
}
