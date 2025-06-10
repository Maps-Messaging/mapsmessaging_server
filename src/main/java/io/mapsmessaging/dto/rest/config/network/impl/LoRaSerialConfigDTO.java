package io.mapsmessaging.dto.rest.config.network.impl;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LoRaSerialConfigDTO extends LoRaConfigDTO {

  @Schema(description = "Serial Device Configuration")
  protected SerialConfigDTO serialConfig;
}
