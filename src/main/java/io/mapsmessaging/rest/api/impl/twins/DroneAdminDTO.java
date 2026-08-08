/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.rest.api.impl.twins;

import io.mapsmessaging.state.config.DroneInfoDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Administrative drone view combining existing DroneInfo configuration with its transport mapping.")
public class DroneAdminDTO {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private DroneInfoDTO drone;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private DroneTransportDTO transport = new DroneTransportDTO();
}
