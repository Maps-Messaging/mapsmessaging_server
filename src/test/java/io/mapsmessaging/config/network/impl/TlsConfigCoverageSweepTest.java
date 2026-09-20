package io.mapsmessaging.config.network.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TlsConfigCoverageSweepTest {
  @Test
  void emptyTlsConfigurationGetsModernDefaultContextAndSerializesSecurityBlock() {
    TlsConfig config = new TlsConfig(new ConfigurationProperties());

    assertNotNull(config.getSslConfig());
    assertEquals("TLSv1.3", config.getSslConfig().getContext());
    assertFalse(config.update(new BaseConfigDTO()));
    assertTrue(config.toConfigurationProperties().containsKey("security"));
  }
}
