package io.mapsmessaging.config.aggregator;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorInputConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AggregatorInputConfigTest {

  @Test
  void topicAndSelectorRoundTrip() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("topicName", "/sensor/+/value");
    properties.put("selector", "quality > 0");

    AggregatorInputConfig restored = new AggregatorInputConfig(new AggregatorInputConfig(properties).toConfigurationProperties());

    assertEquals("/sensor/+/value", restored.getTopicName());
    assertEquals("quality > 0", restored.getSelector());
  }

  @Test
  void blankSelectorIsOmittedFromPackedConfiguration() {
    AggregatorInputConfig config = new AggregatorInputConfig(new ConfigurationProperties());

    ConfigurationProperties packed = config.toConfigurationProperties();

    assertTrue(packed.containsKey("topicName"));
    assertFalse(packed.containsKey("selector"));
  }

  @Test
  void updateAppliesTopicAndSelectorAndRejectsUnrelatedDto() {
    AggregatorInputConfig config = new AggregatorInputConfig(new ConfigurationProperties());
    AggregatorInputConfigDTO updated = new AggregatorInputConfigDTO();
    updated.setTopicName("/updated/#");
    updated.setSelector("state = 'ACTIVE'");

    assertTrue(config.update(updated));
    assertEquals("/updated/#", config.getTopicName());
    assertFalse(config.update(updated));
    assertFalse(config.update(new BaseConfigDTO()));
  }
}