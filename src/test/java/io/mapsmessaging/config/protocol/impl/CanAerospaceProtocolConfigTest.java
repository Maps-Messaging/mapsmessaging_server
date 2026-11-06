package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.protocol.impl.CanAerospaceConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.CoapConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CanAerospaceProtocolConfigTest {

  @Test
  void configuredFieldsRoundTripIncludingOptionalPaths() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("topicNameTemplate", "/ca/{messageName}");
    properties.put("parseToJson", false);
    properties.put("yamlPath", "/tmp/canaerospace.yaml");
    properties.put("unknownPacketTopic", "/ca/unknown");
    properties.put("inboundTopicName", "/ca/in/#");
    properties.put("qualityOfService", 2);
    properties.put("storeOffline", true);

    CanAerospaceProtocolConfig restored =
        new CanAerospaceProtocolConfig(new CanAerospaceProtocolConfig(properties).toConfigurationProperties());

    assertEquals("/ca/{messageName}", restored.getTopicNameTemplate());
    assertFalse(restored.isParseToJson());
    assertEquals("/tmp/canaerospace.yaml", restored.getYamlPath());
    assertEquals("/ca/in/#", restored.getInboundTopicName());
    assertEquals(2, restored.getQualityOfService());
    assertTrue(restored.isStoreOffline());
  }

  @Test
  void updateHandlesNullablePathsAndIsIdempotent() {
    CanAerospaceProtocolConfig config = new CanAerospaceProtocolConfig(new ConfigurationProperties());
    CanAerospaceConfigDTO updated = new CanAerospaceConfigDTO();
    updated.setYamlPath("/schema.yaml");
    updated.setInboundTopicName("/input/#");
    updated.setTopicNameTemplate("/new/{messageName}");
    updated.setUnknownPacketTopic("/new/unknown");
    updated.setParseToJson(false);
    updated.setQualityOfService(1);
    updated.setStoreOffline(true);

    assertTrue(config.update(updated));
    assertEquals("/schema.yaml", config.getYamlPath());
    assertFalse(config.update(updated));
    assertFalse(config.update(new CoapConfigDTO()));
  }
}