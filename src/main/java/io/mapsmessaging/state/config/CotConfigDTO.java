/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.mapsmessaging.state.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Cursor on Target mapping and CoT-only defaults for a source namespace.")
public class CotConfigDTO {

  @Schema(
      description = "Source namespace or MQTT-style namespace filter. '+' matches one level and '#' matches remaining levels.",
      example = "4817/catl/maps/+/+/+")
  private String namespacePath;

  @Schema(
      description = "Affiliation applied to CoT events. SOURCE preserves affiliation supplied by the source event when available.",
      defaultValue = "SOURCE")
  private CotAffiliation affiliation = CotAffiliation.SOURCE;

  @Schema(description = "CoT how value.", defaultValue = "h-g-i-g-o")
  private String how = "h-g-i-g-o";

  @Schema(description = "CoT stale interval in milliseconds.", defaultValue = "30000", minimum = "1")
  private long staleTimeoutMillis = 30_000L;

  @Schema(description = "Optional prefix prepended to generated CoT UIDs.", nullable = true)
  private String uidPrefix;

  @Schema(description = "Default CoT circular error in metres when source accuracy is unavailable.", defaultValue = "10.0", minimum = "0")
  private Double defaultCircularErrorMeters = 10.0d;

  @Schema(description = "Default CoT linear error in metres when source accuracy is unavailable.", defaultValue = "15.0", minimum = "0")
  private Double defaultLinearErrorMeters = 15.0d;

  @Schema(description = "CoT precision-location altitude source.", defaultValue = "GPS")
  private String altitudeSource = "GPS";
}
