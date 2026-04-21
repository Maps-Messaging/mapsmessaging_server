package io.mapsmessaging.state.drone.tak.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "TAK contact information.")
public class TakContact {

  @Schema(description = "Displayed callsign.", example = "Drone 3")
  private String callsign;
}