/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.config.protocol.impl;

import static org.junit.jupiter.api.Assertions.*;

import io.mapsmessaging.configuration.ConfigurationProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MqttSnProtectionConfigTest {
  @Test
  void happyEndpointProtectionRetainsPrivateConfigurationButNotRestDto() throws Exception {
    ConfigurationProperties protection = new ConfigurationProperties(Map.of(
        "enabled", true,
        "counterDirectory", "/tmp/counter-test",
        "senderIdentifier", "0011223344556677",
        "outboundScheme", 0x48));
    MqttSnConfig config = new MqttSnConfig(new ConfigurationProperties(
        Map.of("protection", protection)));
    assertSame(protection, config.getProtectionConfiguration());
    assertSame(protection, config.toConfigurationProperties().get("protection"),
        "Protection must survive server configuration saves");
    String restJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(config);
    assertFalse(restJson.contains("protectionConfiguration"),
        "Keystore protection settings must not leak through REST JSON");
  }

  @Test
  void sadMissingProtectionIsAnUnprotectedEndpoint() {
    MqttSnConfig config = new MqttSnConfig(new ConfigurationProperties(Map.of()));
    assertNull(config.getProtectionConfiguration());
  }

  @Test
  void murphyProtectionNotCopiedThroughDtoOrUpdate() {
    ConfigurationProperties protection = new ConfigurationProperties(Map.of("enabled", false));
    MqttSnConfig config = new MqttSnConfig(new ConfigurationProperties(
        Map.of("protection", protection)));
    MqttSnConfig other = new MqttSnConfig(new ConfigurationProperties(Map.of()));
    config.update(other);
    assertSame(protection, config.getProtectionConfiguration());
    assertNull(other.getProtectionConfiguration());
  }
}
