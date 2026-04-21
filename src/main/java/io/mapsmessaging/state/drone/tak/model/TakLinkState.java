package io.mapsmessaging.state.drone.tak.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Maps-specific link state metadata.")
public class TakLinkState {

  @Schema(description = "Link state.", example = "CONNECTED")
  private String state;

  @Schema(description = "Whether the link is connected.", example = "true")
  private Boolean connected;

  @Schema(description = "RSSI in dBm.", example = "-61")
  private Integer  rssiDbm;

  @Schema(description = "Signal to noise ratio in dB.", example = "18.5")
  private Double snrDb;

  @Schema(description = "Latency in milliseconds.", example = "42")
  private Double latencyMs;

  @Schema(description = "Receive error rate.", example = "0.01")
  private Double rxErrorRate;

  @Schema(description = "Transmit error rate.", example = "0.00")
  private Double txErrorRate;
}