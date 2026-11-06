package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketConfigCoverageSweepTest {
  @Test
  void websocketConfigurationUsesWsTypeAndRejectsUnrelatedDto() {
    WebSocketConfig config = new WebSocketConfig(new ConfigurationProperties());

    assertEquals("ws", config.getType());
    assertFalse(config.update(new BaseConfigDTO()));
    assertNotNull(config.toConfigurationProperties());
  }
}
