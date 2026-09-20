package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExtensionConfigCoverageSweepTest {
  @Test
  void extensionProtocolAndNestedConfigAreLoaded() {
    ConfigurationProperties nested = new ConfigurationProperties();
    nested.put("mode", "fast");
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("protocol", "custom");
    properties.put("config", nested);

    ExtensionConfig config = new ExtensionConfig(properties);

    assertEquals("extension", config.getType());
    assertEquals("custom", config.getProtocol());
    assertEquals("fast", config.getConfig().get("mode"));
    assertFalse(config.update(new BaseConfigDTO()));
    assertNotNull(config.toConfigurationProperties());
  }
}
