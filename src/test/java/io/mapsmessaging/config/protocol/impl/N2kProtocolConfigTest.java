package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.protocol.impl.CoapConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.N2KConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class N2kProtocolConfigTest {

  @Test
  void configuredProtocolFieldsRoundTrip() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("topicNameTemplate", "/n2k/{pgn}");
    properties.put("parseToJson", false);
    properties.put("base64EncodedDatabase", "ZGF0YQ==");
    properties.put("unknownPacketTopic", "/n2k/unknown");
    properties.put("outboundTopicName", "/n2k/out/#");
    properties.put("canBusAddress", 42);
    properties.put("qualityOfService", 2);
    properties.put("storeOffline", true);

    N2kProtocolConfig restored = new N2kProtocolConfig(new N2kProtocolConfig(properties).toConfigurationProperties());

    assertEquals("/n2k/{pgn}", restored.getTopicNameTemplate());
    assertFalse(restored.isParseToJson());
    assertEquals("ZGF0YQ==", restored.getBase64EncodedDatabase());
    assertEquals(42, restored.getCanBusAddress());
    assertEquals(2, restored.getQualityOfService());
    assertTrue(restored.isStoreOffline());
  }

  @Test
  void updateAppliesN2kFieldsAndRejectsUnrelatedDto() {
    N2kProtocolConfig config = new N2kProtocolConfig(new ConfigurationProperties());
    N2KConfigDTO updated = new N2KConfigDTO();
    updated.setTopicNameTemplate("/changed/{pgn}");
    updated.setParseToJson(false);
    updated.setUnknownPacketTopic("/changed/unknown");
    updated.setOutboundTopicName("/changed/out/#");
    updated.setCanBusAddress(7);
    updated.setQualityOfService(1);
    updated.setStoreOffline(true);

    assertTrue(config.update(updated));
    assertEquals(7, config.getCanBusAddress());
    assertEquals("/changed/out/#", config.getOutboundTopicName());
    assertFalse(config.update(updated));
    assertFalse(config.update(new CoapConfigDTO()));
  }
}