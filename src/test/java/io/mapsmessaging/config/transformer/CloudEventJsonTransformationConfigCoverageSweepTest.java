package io.mapsmessaging.config.transformer;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CloudEventJsonTransformationConfigCoverageSweepTest {
  @Test
  void constructorSetsJsonCloudEventTransformationType() {
    assertEquals(
        TransformationType.CLOUD_EVENT_JSON,
        new CloudEventJsonTransformationConfig(new ConfigurationProperties()).getType());
  }
}
