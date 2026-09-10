/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.dto.rest.config.protocol.impl;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "TAK client presence event sent after connecting to a remote TAK server.")
public class CotPresenceConfigDTO extends BaseConfigDTO {

  @Schema(description = "Send and periodically refresh the TAK client presence event.", defaultValue = "false")
  protected boolean enabled;

  @Schema(description = "Stable CoT identity. Supports {interfaceName}.", example = "maps-{interfaceName}")
  protected String uid = "maps-{interfaceName}";

  @Schema(description = "TAK callsign. Supports {interfaceName}.", example = "MAPS-{interfaceName}")
  protected String callsign = "MAPS-{interfaceName}";

  @Schema(description = "CoT type used for the Maps gateway presence.", defaultValue = "a-f-G-E")
  protected String cotType = "a-f-G-E";

  @Schema(description = "CoT source/derivation method.", defaultValue = "m-g")
  protected String how = "m-g";

  @Schema(description = "Presence latitude in decimal degrees.", defaultValue = "0")
  protected double latitude;

  @Schema(description = "Presence longitude in decimal degrees.", defaultValue = "0")
  protected double longitude;

  @Schema(description = "Height above ellipsoid in metres.", defaultValue = "0")
  protected double hae;

  @Schema(description = "Circular error in metres; 9999999 indicates unknown.", defaultValue = "9999999")
  protected double ce = 9_999_999;

  @Schema(description = "Linear error in metres; 9999999 indicates unknown.", defaultValue = "9999999")
  protected double le = 9_999_999;

  @Schema(description = "TAK group/team name.", defaultValue = "Cyan")
  protected String groupName = "Cyan";

  @Schema(description = "TAK group role.", defaultValue = "Team Member")
  protected String groupRole = "Team Member";

  @Schema(description = "TAK device description.", defaultValue = "MapsMessaging")
  protected String device = "MapsMessaging";

  @Schema(description = "TAK platform description.", defaultValue = "MapsMessaging")
  protected String platform = "MapsMessaging";

  @Schema(description = "TAK operating-system description.", defaultValue = "Java")
  protected String operatingSystem = "Java";

  @Schema(description = "TAK software version; omitted when blank.")
  protected String softwareVersion = "";

  @Schema(description = "Seconds between presence events.", defaultValue = "60", minimum = "1")
  protected int intervalSeconds = 60;

  @Schema(description = "Seconds after generation when a presence event becomes stale.", defaultValue = "120", minimum = "2")
  protected int staleSeconds = 120;
}
