package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AmqpConfigCoverageSweepTest {
  @Test
  void amqpSpecificFieldsParseAndSerialize() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("idleTimeout", 10);
    properties.put("maxFrameSize", 2048);
    properties.put("linkCredit", 50);
    properties.put("durable", true);
    properties.put("incomingCapacity", 100);
    properties.put("outgoingWindow", 200);

    AmqpConfig config = new AmqpConfig(properties);

    assertEquals("amqp", config.getType());
    assertEquals(10, config.getIdleTimeout());
    assertEquals(2048, config.getMaxFrameSize());
    assertEquals(50, config.getLinkCredit());
    assertTrue(config.isDurable());
    assertFalse(config.update(new BaseConfigDTO()));
    assertEquals(200, config.toConfigurationProperties().getIntProperty("outgoingWindow", -1));
  }
}
