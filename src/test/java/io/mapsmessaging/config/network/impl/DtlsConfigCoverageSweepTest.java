package io.mapsmessaging.config.network.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DtlsConfigCoverageSweepTest {
  @Test
  void defaultsToDtlsContextAndRejectsUnrelatedUpdates() {
    DtlsConfig config = new DtlsConfig(new ConfigurationProperties());

    assertEquals("dtls", config.getType());
    assertNotNull(config.getSslConfig());
    assertTrue(config.getSslConfig().getContext().toLowerCase().startsWith("dtls"));
    assertFalse(config.update(new BaseConfigDTO()));
    assertTrue(config.toConfigurationProperties().containsKey("security"));
  }
}
