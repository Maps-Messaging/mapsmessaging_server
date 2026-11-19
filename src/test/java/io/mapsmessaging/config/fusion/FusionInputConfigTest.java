package io.mapsmessaging.config.aggregator;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.aggregator.FusionInputConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FusionInputConfigTest {

  @Test
  void topicAndSelectorRoundTrip() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("topicName", "/sensor/+/value");
    properties.put("selector", "quality > 0");

    FusionInputConfig restored = new FusionInputConfig(new FusionInputConfig(properties).toConfigurationProperties());

    assertEquals("/sensor/+/value", restored.getTopicName());
    assertEquals("quality > 0", restored.getSelector());
  }

  @Test
  void blankSelectorIsOmittedFromPackedConfiguration() {
    FusionInputConfig config = new FusionInputConfig(new ConfigurationProperties());

    ConfigurationProperties packed = config.toConfigurationProperties();

    assertTrue(packed.containsKey("topicName"));
    assertFalse(packed.containsKey("selector"));
  }

  @Test
  void updateAppliesTopicAndSelectorAndRejectsUnrelatedDto() {
    FusionInputConfig config = new FusionInputConfig(new ConfigurationProperties());
    FusionInputConfigDTO updated = new FusionInputConfigDTO();
    updated.setTopicName("/updated/#");
    updated.setSelector("state = 'ACTIVE'");

    assertTrue(config.update(updated));
    assertEquals("/updated/#", config.getTopicName());
    assertFalse(config.update(updated));
    assertFalse(config.update(new BaseConfigDTO()));
  }
}