package io.mapsmessaging.config.transformer;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonToXmlTransformationConfigCoverageSweepTest {
  @Test
  void constructorSetsJsonToXmlType() {
    assertEquals(
        TransformationType.JSON_TO_XML,
        new JsonToXmlTransformationConfig(new ConfigurationProperties()).getType());
  }
}
