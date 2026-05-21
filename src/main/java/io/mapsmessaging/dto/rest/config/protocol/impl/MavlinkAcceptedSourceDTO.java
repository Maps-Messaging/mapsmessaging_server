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

package io.mapsmessaging.dto.rest.config.protocol.impl;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
@Schema(
    name = "MavlinkAcceptedSourceDTO",
    description = "Defines a MAVLink source accepted by the MAVLink protocol layer."
)
public class MavlinkAcceptedSourceDTO {

  @Schema(
      description = "MAVLink system ID accepted by this source entry.",
      example = "1",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private int systemId;

  @Schema(
      description = "MAVLink component ID accepted by this source entry.",
      example = "1",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private int componentId;

  @Schema(
      description = "MAVLink message IDs accepted from this source. Empty means the source uses the protocol-level accepted message ID list.",
      example = "[0, 1, 30, 33]"
  )
  private List<Integer> acceptedMessageIds = new ArrayList<>();

  @Schema(
      description = "MAVLink message IDs rejected from this source. Applied after acceptedMessageIds. Empty means the protocol-level rejectedMessageIds list is used.",
      example = "[411]"
  )
  private List<Integer> rejectedMessageIds = new ArrayList<>();
}