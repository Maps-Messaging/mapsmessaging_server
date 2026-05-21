package io.mapsmessaging.dto.rest.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Represents a named configuration mapping.")
public final class ConfigNamingDTO {

  @Schema(
      description = "Human-readable name for this configuration mapping.",
      example = "Networks",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private String name;

  @Schema(
      description = "Internal configuration name this mapping is bound to.",
      example = "NetworkManageronfig",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private String configName;
}