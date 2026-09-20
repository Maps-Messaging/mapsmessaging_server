package io.mapsmessaging.config.transformer;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XmlToJsonTransformationConfigCoverageSweepTest {
  @Test
  void constructorSetsXmlToJsonType() {
    assertEquals(
        TransformationType.XML_TO_JSON,
        new XmlToJsonTransformationConfig(new ConfigurationProperties()).getType());
  }
}
