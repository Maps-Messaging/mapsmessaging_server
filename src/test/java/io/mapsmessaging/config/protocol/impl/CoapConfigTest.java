package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.protocol.impl.CoapConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.NatsConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoapConfigTest {

  @Test
  void configuredBlockAndIdleSettingsRoundTrip() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("maxBlockSize", 1024);
    properties.put("idleTimePeriod", 45);

    CoapConfig restored = new CoapConfig(new CoapConfig(properties).toConfigurationProperties());

    assertEquals(1024, restored.getMaxBlockSize());
    assertEquals(45, restored.getIdleTime());
  }

  @Test
  void updateAppliesBothFieldsAndRejectsUnrelatedDto() {
    CoapConfig config = new CoapConfig(new ConfigurationProperties());
    CoapConfigDTO updated = new CoapConfigDTO();
    updated.setMaxBlockSize(2048);
    updated.setIdleTime(77);

    assertTrue(config.update(updated));
    assertEquals(2048, config.getMaxBlockSize());
    assertEquals(77, config.getIdleTime());
    assertFalse(config.update(updated));
    assertFalse(config.update(new NatsConfigDTO()));
  }
}