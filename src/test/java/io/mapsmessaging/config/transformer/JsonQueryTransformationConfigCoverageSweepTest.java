package io.mapsmessaging.config.transformer;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonQueryTransformationConfigCoverageSweepTest {
  @Test
  void nestedParametersOverrideOuterQuery() {
    ConfigurationProperties parameters = new ConfigurationProperties();
    parameters.put("query", "$.vehicle.position");
    ConfigurationProperties root = new ConfigurationProperties();
    root.put("query", "$.ignored");
    root.put("parameters", parameters);

    JsonQueryTransformationConfig config = new JsonQueryTransformationConfig(root);

    assertEquals(TransformationType.JSON_QUERY, config.getType());
    assertEquals("$.vehicle.position", config.getQuery());
  }
}
