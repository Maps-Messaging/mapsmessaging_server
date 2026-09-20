package io.mapsmessaging.config.network.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UdpConfigCoverageSweepTest {
  @Test
  void udpConfigurationUsesUdpTypeAndRejectsUnrelatedUpdate() {
    UdpConfig config = new UdpConfig(new ConfigurationProperties());

    assertEquals("udp", config.getType());
    assertFalse(config.update(new BaseConfigDTO()));
    assertNotNull(config.toConfigurationProperties());
  }
}
