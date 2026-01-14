package io.mapsmessaging.rest.responses;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.ConfigurationManagerDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Data
@AllArgsConstructor
@Schema(
    name = "ConfigurationSchema",
    description = "Configuration object together with its JSON Schema (Draft 2020-12)."
)
public class ConfigurationSchemaDTO {

  @Schema(
      description = "Configuration object. The concrete shape is selected by the discriminator field 'type'.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      implementation = ConfigurationManagerDTO.class
  )
  private BaseConfigDTO config;

  @Schema(
      description = "JSON Schema describing the configuration object.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      type = "object",
      additionalProperties = Schema.AdditionalPropertiesValue.TRUE
  )
  private Map<String, Object> schema;
}
