package io.mapsmessaging.dto.rest.config.transformer.jsonmapper;

import com.google.gson.JsonElement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Maps a value from one JSON path to another, with an optional transformation.")
public class JsonMapOpDTO {

  @Schema(
      description = "Source JSON path.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      example = "position.latitude"
  )
  private String from;

  @Schema(
      description = "Target JSON path.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false,
      defaultValue = "",
      example = "stanag.location.lat")
  private String to;

  @Schema(
      description = "Optional transform function.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  private JsonMapFunction function = JsonMapFunction.NONE;

  @Schema(
      description = "Default value used when the source path is missing.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      defaultValue = "null"
  )
  private JsonElement defaultValue;

  @Schema(
      description = "If true, silently skip the mapping when the source value is missing.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      defaultValue = "true"
  )
  private boolean ignoreMissing = true;
}