package io.mapsmessaging.state.drone.tak.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "TAK track information.")
public class TakTrack {

  @Schema(description = "Speed in meters per second.", example = "12.4")
  private Double speed;

  @Schema(description = "Course over ground in degrees.", example = "96.72")
  private Double course;
}