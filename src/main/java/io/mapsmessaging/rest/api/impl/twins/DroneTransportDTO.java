/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.rest.api.impl.twins;

import io.mapsmessaging.state.config.VehicleClass;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Transport mapping used to bind persisted drone information to an existing MAVLink or CAN/N2K configuration.")
public class DroneTransportDTO {

  public enum Type {
    MAVLINK,
    CANBUS,
    NONE
  }

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Type type = Type.NONE;

  @Schema(description = "Existing MAVLink configuration name containing this drone's known-source mapping.")
  private String mavlinkSourceName;

  @Schema(description = "MAVLink system identifier.", minimum = "1", maximum = "255")
  private Integer systemId;

  @Schema(description = "MAVLink component identifier.", minimum = "0", maximum = "255")
  private Integer componentId;

  private VehicleClass vehicleClass;

  @Schema(description = "CAN/N2K subscription topic. Used only when type is CANBUS.")
  private String topic;
}
