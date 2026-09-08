/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.config.protocol.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.config.network.EndPointConnectionServerConfig;
import io.mapsmessaging.config.network.EndPointServerConfig;
import io.mapsmessaging.config.network.impl.TcpConfig;
import io.mapsmessaging.config.network.impl.TlsConfig;
import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;
import java.util.List;
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

  @Test
  void network_connection_loads_cot_push_and_pull_bindings_for_tcp_and_tls() {
    for (String url : new String[]{"tcp://tak.example:8088/", "ssl://tak.example:8089/"}) {
      ConfigurationProperties push = new ConfigurationProperties();
      push.put("direction", "push");
      push.put("local_namespace", "/tak/cot/outbound");
      push.put("remote_namespace", "/cot");
      push.put("qos", 1);

      ConfigurationProperties pull = new ConfigurationProperties();
      pull.put("direction", "pull");
      pull.put("remote_namespace", "/cot");
      pull.put("local_namespace", "/tak/cot/inbound");
      pull.put("qos", 1);

      ConfigurationProperties properties = new ConfigurationProperties();
      properties.put("name", "remote-tak");
      properties.put("url", url);
      properties.put("protocol", "cot");
      properties.put("links", List.of(push, pull));

      EndPointConnectionServerConfig endpoint = new EndPointConnectionServerConfig(properties);
      assertInstanceOf(CotConfig.class, endpoint.getProtocolConfig("cot"));
      if (url.startsWith("ssl")) {
        assertInstanceOf(TlsConfig.class, endpoint.getEndPointConfig());
      } else {
        assertInstanceOf(TcpConfig.class, endpoint.getEndPointConfig());
      }

      assertEquals(2, endpoint.getLinkConfigs().size());
      LinkConfigDTO pushBinding = endpoint.getLinkConfigs().get(0);
      assertEquals("push", pushBinding.getDirection());
      assertEquals("/tak/cot/outbound", pushBinding.getLocalNamespace());
      LinkConfigDTO pullBinding = endpoint.getLinkConfigs().get(1);
      assertEquals("pull", pullBinding.getDirection());
      assertEquals("/tak/cot/inbound", pullBinding.getLocalNamespace());
    }
  }
}
