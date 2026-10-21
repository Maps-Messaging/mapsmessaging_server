package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.protocol.impl.CoapConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.NatsConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NatsConfigTest {

  @Test
  void configuredValuesRoundTrip() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("maximumBufferSize", 1024);
    properties.put("maximumReceive", 77);
    properties.put("enableStreams", true);
    properties.put("enableObjectStore", true);
    properties.put("enableKeyValues", true);
    properties.put("keepAlive", 1234);
    properties.put("namespaceRoot", "/nats");
    properties.put("enableStreamDelete", false);

    NatsConfig restored = new NatsConfig(new NatsConfig(properties).toConfigurationProperties());

    assertEquals(1024, restored.getMaxBufferSize());
    assertEquals(77, restored.getMaxReceive());
    assertTrue(restored.isEnableStreams());
    assertTrue(restored.isEnableObjectStore());
    assertTrue(restored.isEnableKeyValues());
    assertEquals(1234, restored.getKeepAlive());
    assertEquals("/nats", restored.getNamespaceRoot());
    assertFalse(restored.isEnableStreamDelete());
  }

  @Test
  void updateAppliesFeatureSwitchesAndRejectsUnrelatedDto() {
    NatsConfig config = new NatsConfig(new ConfigurationProperties());
    NatsConfigDTO updated = new NatsConfigDTO();
    updated.setMaxBufferSize(2048);
    updated.setMaxReceive(10);
    updated.setEnableStreams(true);
    updated.setEnableObjectStore(true);
    updated.setEnableKeyValues(true);
    updated.setKeepAlive(999);
    updated.setNamespaceRoot("/root");
    updated.setEnableStreamDelete(false);

    assertTrue(config.update(updated));
    assertEquals("/root", config.getNamespaceRoot());
    assertFalse(config.update(updated));
    assertFalse(config.update(new CoapConfigDTO()));
  }
}