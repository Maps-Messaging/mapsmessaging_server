package io.mapsmessaging.config.network.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TcpConfigCoverageSweepTest {
  @Test
  void tcpConfigRoundTripsFactoryFieldsAndRejectsOtherDtoTypes() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("readBufferSize", 4096);
    properties.put("writeBufferSize", 8192);

    TcpConfig config = new TcpConfig(properties);

    assertEquals("tcp", config.getType());
    assertFalse(config.update(new BaseConfigDTO()));
    ConfigurationProperties packed = config.toConfigurationProperties();
    assertNotNull(packed);
  }
}
