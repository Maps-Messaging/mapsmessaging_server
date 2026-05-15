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

package io.mapsmessaging.dto.rest.config.twin;

import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkKnownSourceDTO;
import io.mapsmessaging.state.mavlink.MavlinkStateSubscriptionMode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Configuration for consuming MAVLink state from a topic source and applying it to the twin manager.
 */
@Data
@Schema(
    name = "MavlinkTwinConfigDTO",
    description = "Configuration for processing MAVLink state messages from a topic source into the twin manager."
)
public class MavlinkTwinConfigDTO {

  @Schema(
      description = "Logical name for this MAVLink twin state source. Used for identification and diagnostics.",
      example = "mavlink-state"
  )
  private String name = "mavlink";

  @Schema(
      description = "Topic path or namespace subscription used to receive MAVLink state messages.",
      example = "/mavlink/>",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private String topic;

  @Schema(
      description = "MAVLink dialect name or dialect file path. If omitted, the default/common dialect is used.",
      example = "common"
  )
  private String dialectName;

  @Schema(
      description = "Controls whether MAVLink messages update twin state, plan state, or both.",
      defaultValue = "TWIN",
      implementation = MavlinkStateSubscriptionMode.class
  )
  private MavlinkStateSubscriptionMode subscriptionMode = MavlinkStateSubscriptionMode.TWIN;

  @Schema(
      description = "Known MAVLink sources for this topic source. Only listed sources are processed into twins. Unknown sources are ignored."
  )
  private List<MavlinkKnownSourceDTO> knownSources = new ArrayList<>();
}