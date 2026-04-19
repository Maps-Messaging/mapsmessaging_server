package io.mapsmessaging.dto.rest.config.protocol.impl;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "Known MAVLink source definition.")
public class MavlinkKnownSourceDTO {

  @Schema(
      description = "Friendly name for the source.",
      example = "drone-1"
  )
  protected String name = "";

  @Schema(
      description = "Optional source description.",
      example = "Primary aircraft autopilot"
  )
  protected String description = "";

  @Schema(description = "MAVLink system ID.", example = "1", minimum = "1", maximum = "255")
  protected int systemId;

  @Schema(description = "MAVLink component ID.", example = "1", minimum = "0", maximum = "255")
  protected int componentId;

  @Schema(
      description = "Vehicle class (UAV=air, USV=surface, UGV=ground, UUV=underwater, GCS=control)."
  )
  protected VehicleClass vehicleClass;

  @ArraySchema(
      schema = @Schema(description = "Per-source allow-list of message IDs.")
  )
  protected List<Integer> acceptedMessageIds = new ArrayList<>();

  @ArraySchema(
      schema = @Schema(description = "Per-source reject-list of message IDs.")
  )
  protected List<Integer> rejectedMessageIds = new ArrayList<>();
}