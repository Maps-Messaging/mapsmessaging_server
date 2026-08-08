/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.rest.api.impl.twins;

import io.mapsmessaging.state.mavlink.model.UxvOperation;
import io.mapsmessaging.state.mavlink.model.UxvVehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Installed UxV model and the operations it reports as supported.")
public class UxvModelSummaryDTO {

  private String name;
  private UxvVehicleType vehicleType;
  private Set<UxvOperation> supportedOperations;
}
