package io.mapsmessaging.config.transformer;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CloudEventEnvelopeTransformationConfigCoverageSweepTest {
  @Test
  void constructorSetsEnvelopeTransformationType() {
    assertEquals(
        TransformationType.CLOUD_EVENT_ENVELOPE,
        new CloudEventEnvelopeTransformationConfig(new ConfigurationProperties()).getType());
  }
}
