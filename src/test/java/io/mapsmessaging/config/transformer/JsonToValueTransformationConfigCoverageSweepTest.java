package io.mapsmessaging.config.transformer;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonToValueTransformationConfigCoverageSweepTest {
  @Test
  void legacyDataKeyIsUsedWhenKeyIsMissing() {
    ConfigurationProperties parameters = new ConfigurationProperties();
    parameters.put("data", "vehicle.id");
    ConfigurationProperties root = new ConfigurationProperties();
    root.put("parameters", parameters);

    JsonToValueTransformationConfig config = new JsonToValueTransformationConfig(root);

    assertEquals(TransformationType.JSON_TO_VALUE, config.getType());
    assertEquals("vehicle.id", config.getKey());
  }
}
