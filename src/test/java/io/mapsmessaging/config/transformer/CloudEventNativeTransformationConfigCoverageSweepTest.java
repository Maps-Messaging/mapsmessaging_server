package io.mapsmessaging.config.transformer;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CloudEventNativeTransformationConfigCoverageSweepTest {
  @Test
  void constructorSetsNativeCloudEventTransformationType() {
    assertEquals(
        TransformationType.CLOUD_EVENT_NATIVE,
        new CloudEventNativeTransformationConfig(new ConfigurationProperties()).getType());
  }
}
