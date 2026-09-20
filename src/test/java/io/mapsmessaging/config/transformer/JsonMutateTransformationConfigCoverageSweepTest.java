package io.mapsmessaging.config.transformer;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import io.mapsmessaging.dto.rest.config.transformer.jsonmutate.JsonMutateOperation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonMutateTransformationConfigCoverageSweepTest {
  @Test
  void validSetAndInvalidOperationsAreFiltered() {
    ConfigurationProperties set = new ConfigurationProperties();
    set.put("op", "set");
    set.put("path", "state.mode");
    set.put("value", "AUTO");

    ConfigurationProperties invalid = new ConfigurationProperties();
    invalid.put("op", "not-real");

    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("operations", List.of(set, invalid));

    JsonMutateTransformationConfig config = new JsonMutateTransformationConfig(properties);

    assertEquals(TransformationType.JSON_MUTATE, config.getType());
    assertEquals(1, config.getOperations().size());
    assertEquals(JsonMutateOperation.SET, config.getOperations().getFirst().getOp());
    assertEquals("state.mode", config.getOperations().getFirst().getPath());
    assertEquals("AUTO", config.getOperations().getFirst().getValue().getAsString());
  }
}
