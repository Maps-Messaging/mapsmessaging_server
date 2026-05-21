package io.mapsmessaging.state.drone.tak.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "TAK detail element.")
public class TakDetail {

  @Schema(description = "Contact information.")
  private TakContact contact;

  @Schema(description = "Track information including speed and course.")
  private TakTrack track;

  @Schema(description = "Lifecycle status information.")
  private TakStatus status;

  @Schema(description = "Free text remarks.", example = "mode=GUIDED | mission=ACTIVE")
  private String remarks;

  @Schema(description = "Altitude source metadata.")
  private TakPrecisionLocation precisionLocation;

  @Schema(description = "TAK client/platform information.")
  private TakPlatform takv;

  @Schema(description = "Link health metadata.")
  private TakLinkState mapsLink;

  @ArraySchema(arraySchema = @Schema(description = "Relationship links to other TAK entities."))
  private List<TakLink> links = new ArrayList<>();
}