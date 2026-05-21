package io.mapsmessaging.state.drone.tak.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "TAK lifecycle status.")
public class TakStatus {

  @Schema(description = "Lifecycle state.", example = "ACTIVE")
  private String lifecycle;

  @Schema(description = "Reason for the current status.", example = "updated")
  private String reason;
}