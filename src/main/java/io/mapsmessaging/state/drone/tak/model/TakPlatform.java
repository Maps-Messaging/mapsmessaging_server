package io.mapsmessaging.state.drone.tak.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "TAK platform metadata.")
public class TakPlatform {

  @Schema(description = "Device type.", example = "DRONE")
  private String device;

  @Schema(description = "Platform name or vehicle class.", example = "UAV")
  private String platform;

  @Schema(description = "Operating system or producer.", example = "MapsMessaging")
  private String os;

  @Schema(description = "Producer version.", example = "1.0")
  private String version;
}