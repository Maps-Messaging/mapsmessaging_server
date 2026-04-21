package io.mapsmessaging.state.drone.tak.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Relationship link to another TAK entity.")
public class TakLink {

  @Schema(description = "Target TAK UID.", example = "gcs-1")
  private String uid;

  @Schema(description = "Relationship type.", example = "controlled-by")
  private String relation;
}