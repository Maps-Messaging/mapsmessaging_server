package io.mapsmessaging.config.transformer;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonToValueTransformationDTO;

public class JsonToValueTransformationConfig extends JsonToValueTransformationDTO {

  public JsonToValueTransformationConfig(ConfigurationProperties props) {
    setType(TransformationType.JSON_TO_VALUE);

    ConfigurationProperties parameters = unwrapParameters(props);

    String key = parameters.getProperty("key", null);
    if (key == null) {
      key = parameters.getProperty("data", null);
    }
    setKey(key);
  }

  private ConfigurationProperties unwrapParameters(ConfigurationProperties props) {
    Object raw = props.get("parameters");
    if (raw instanceof ConfigurationProperties properties) {
      return properties;
    }
    return props;
  }
}
