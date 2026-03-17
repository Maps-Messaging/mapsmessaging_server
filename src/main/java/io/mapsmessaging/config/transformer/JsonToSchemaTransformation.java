package io.mapsmessaging.config.transformer;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonToSchemaTransformationDTO;

public class JsonToSchemaTransformation extends JsonToSchemaTransformationDTO {

  public JsonToSchemaTransformation(ConfigurationProperties props) {
    setType(TransformationType.JSON_TO_SCHEMA);
    ConfigurationProperties parameters = unwrapParameters(props);
    schemaName = parameters.getProperty("schema", null);
  }

  private ConfigurationProperties unwrapParameters(ConfigurationProperties props) {
    Object raw = props.get("parameters");
    if (raw instanceof ConfigurationProperties properties) {
      return properties;
    }
    return props;
  }
}
