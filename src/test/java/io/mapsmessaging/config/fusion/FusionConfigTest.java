package io.mapsmessaging.config.fusion;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.fusion.FusionConfigDTO;
import io.mapsmessaging.dto.rest.config.fusion.WindowCloseMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FusionConfigTest {

  @Test
  void parsesMapInputsAndCaseInsensitiveWindowMode() {
    ConfigurationProperties properties = baseProperties();
    properties.put("windowCloseMode", "timeout_only");
    properties.put("inputs", List.of(Map.of("topicName", "/sensor/+/value", "selector", "quality > 0")));

    FusionConfig config = new FusionConfig(properties);

    assertEquals(WindowCloseMode.TIMEOUT_ONLY, config.getWindowCloseMode());
    assertEquals(1, config.getInputs().size());
    assertEquals("/sensor/+/value", config.getInputs().getFirst().getTopicName());
  }

  @Test
  void roundTripPreservesEmitFirstEventImmediately() {
    ConfigurationProperties properties = baseProperties();
    properties.put("emitFirstEventImmediately", true);
    ConfigurationProperties input = new ConfigurationProperties();
    input.put("topicName", "/sensor/#");
    properties.put("inputs", input);

    FusionConfig source = new FusionConfig(properties);
    FusionConfig restored = new FusionConfig(source.toConfigurationProperties());

    assertTrue(restored.isEmitFirstEventImmediately());
  }

  @Test
  void updateAppliesScalarChangesAndThenBecomesIdempotent() {
    FusionConfig config = new FusionConfig(baseProperties());
    FusionConfigDTO updated = new FusionConfigDTO();
    updated.setName("updated");
    updated.setEnabled(false);
    updated.setOutputTopic("/new/out");
    updated.setWindowCloseMode(WindowCloseMode.ALL_INPUTS);
    updated.setWindowDurationMs(2000);
    updated.setTimeoutMs(3000);
    updated.setMaxEventsPerTopic(4);
    updated.setInputs(config.getInputs());
    updated.setOutputTransformers(config.getOutputTransformers());

    assertTrue(config.update(updated));
    assertEquals("updated", config.getName());
    assertEquals(WindowCloseMode.ALL_INPUTS, config.getWindowCloseMode());
    assertFalse(config.update(updated));
    assertFalse(config.update(new BaseConfigDTO()));
  }

  private static ConfigurationProperties baseProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", "agg");
    properties.put("enabled", true);
    properties.put("outputTopic", "/out");
    properties.put("windowDurationMs", 1000L);
    properties.put("timeoutMs", 1500L);
    properties.put("maxEventsPerTopic", 1);
    return properties;
  }
}