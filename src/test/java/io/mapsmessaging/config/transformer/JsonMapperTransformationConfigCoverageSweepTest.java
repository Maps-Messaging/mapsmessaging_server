package io.mapsmessaging.config.transformer;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonMapperTransformationConfigCoverageSweepTest {
  @Test
  void directOperationListBuildsOnlyConfigurationPropertyEntries() {
    ConfigurationProperties operation = new ConfigurationProperties();
    operation.put("from", "source.value");
    operation.put("to", "target.value");
    operation.put("function", "NONE");
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("operations", List.of(operation, "ignored"));

    JsonMapperTransformationConfig config = new JsonMapperTransformationConfig(properties);

    assertEquals(TransformationType.JSON_MAPPER, config.getType());
    assertEquals(1, config.getOperations().size());
    assertEquals("source.value", config.getOperations().getFirst().getFrom());
    assertEquals("target.value", config.getOperations().getFirst().getTo());
  }
}
