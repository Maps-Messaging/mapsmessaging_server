package io.mapsmessaging.dto.rest.config.transformer.jsonmapper;

import com.google.gson.JsonElement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Maps a value from one JSON path to another, with an optional transformation.")
public class JsonMapOpDTO {

  @Schema(description = "Source JSON path.", example = "position.latitude")
  private String from;

  @Schema(description = "Target JSON path.", example = "stanag.location.lat")
  private String to;

  @Schema(description = "Optional transform function.")
  private JsonMapFunction function = JsonMapFunction.NONE;

  @Schema(description = "Default value used when the source path is missing.")
  private JsonElement defaultValue;

  @Schema(description = "If true, silently skip the mapping when the source value is missing.")
  private boolean ignoreMissing = true;
}