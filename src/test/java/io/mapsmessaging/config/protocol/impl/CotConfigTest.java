/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.config.protocol.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.config.network.EndPointServerConfig;
import org.junit.jupiter.api.Test;

class CotConfigTest {

  @Test
  void loadsAndSerialisesConfiguration() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("inboundTopicName", "/cot/from-wire");
    properties.put("outboundTopicName", "/cot/to-wire");
    properties.put("maximumEventSize", 4096);
    properties.put("maximumSessionExpiry", 60);
    properties.put("qualityOfService", 1);
    properties.put("storeOffline", true);
    properties.put("appendNewLine", false);

    CotConfig config = new CotConfig(properties);
    assertEquals("cot", config.getType());
    assertEquals("/cot/from-wire", config.getInboundTopicName());
    assertEquals(4096, config.getMaximumEventSize());
    assertFalse(config.isAppendNewLine());

    ConfigurationProperties saved = config.toConfigurationProperties();
    assertEquals("/cot/to-wire", saved.getProperty("outboundTopicName"));
    assertEquals(1, saved.getIntProperty("qualityOfService", 0));
  }

  @Test
  void endpointFactoryLoadsCotForTcpAndTls() {
    for (String url : new String[]{"tcp://127.0.0.1:8088/", "ssl://127.0.0.1:8089/"}) {
      ConfigurationProperties properties = new ConfigurationProperties();
      properties.put("name", "tak");
      properties.put("url", url);
      properties.put("protocol", "cot");
      EndPointServerConfig endpoint = new EndPointServerConfig(properties);
      assertEquals("cot", endpoint.getProtocolConfig("cot").getType());
      assertEquals(CotConfig.class, endpoint.getProtocolConfig("cot").getClass());
    }
  }
}
