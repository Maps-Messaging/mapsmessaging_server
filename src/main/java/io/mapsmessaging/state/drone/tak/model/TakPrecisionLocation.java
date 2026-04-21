package io.mapsmessaging.state.drone.tak.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Precision location metadata.")
public class TakPrecisionLocation {

  @Schema(description = "Altitude source.", example = "GPS")
  private String altsrc;
}