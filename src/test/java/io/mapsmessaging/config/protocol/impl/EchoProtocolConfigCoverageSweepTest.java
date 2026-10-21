package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EchoProtocolConfigCoverageSweepTest {
  @Test
  void constructorForcesEchoProtocolType() {
    EchoProtocolConfig config = new EchoProtocolConfig(new ConfigurationProperties());

    assertEquals("echo", config.getType());
  }
}
