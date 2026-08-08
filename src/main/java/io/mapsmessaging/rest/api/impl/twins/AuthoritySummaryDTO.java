/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.rest.api.impl.twins;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Derived STANAG authority usage across configured drone task capabilities.")
public class AuthoritySummaryDTO {

  private UUID uuid;
  private List<DroneBinding> drones = new ArrayList<>();

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DroneBinding {
    private String name;
    private int assignedCapabilities;
    private int totalCapabilities;
  }
}
