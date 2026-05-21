package io.mapsmessaging.config.transformer;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;

public interface LegacyTransformationLoader {
  TransformationConfigDTO fromLegacy(ConfigurationProperties legacy);
}
