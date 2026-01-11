package io.mapsmessaging.rest.responses;

import com.google.gson.JsonObject;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConfigurationSchemaDTO {
  private BaseConfigDTO config;
  private JsonObject schema;
}