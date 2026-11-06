package io.mapsmessaging.dto.rest.config.protocol.impl;

import io.mapsmessaging.state.config.VehicleClass;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

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

  @Schema(
      description = "Overrides the CoT type's classification segment (everything after "
          + "\"a-<affiliation>-\") for this specific asset, instead of the generic "
          + "vehicleClass-derived default - for when this asset isn't really representative of "
          + "its configured vehicleClass (e.g. an unmanned platform sharing vehicleClass: USV "
          + "with a real manned boat that should render differently). Example: \"S-C-U\" "
          + "(Sea Surface, Combatant, Unmanned).",
      example = "S-C-U",
      nullable = true
  )
  protected String cotClassification;

}