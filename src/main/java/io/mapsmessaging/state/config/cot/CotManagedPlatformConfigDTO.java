/*
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
package io.mapsmessaging.state.config.cot;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "A configured endpoint-scoped CoT identity bound to a canonical twin.")
public class CotManagedPlatformConfigDTO {

  @Schema(description = "Stable CoT endpoint name added to inbound message metadata.")
  private String endpoint;

  @Schema(description = "Inbound CoT UID within the endpoint namespace.")
  private String uid;

  @Schema(description = "Canonical configured twin name.")
  private String twinId;

  @Schema(description = "UID emitted to CoT for this twin. Defaults to uid.")
  private String outboundUid;

  @Schema(description = "Named tasking profile understood by the remote CoT component.")
  private String taskingProfile = "maps-stanag-4817-v1";

  @Schema(description = "MSL minus HAE in metres. Required when altitude is mapped.")
  private Double haeToMslOffsetMeters;

  @Schema(description = "Source precedence used for CoT observations. Higher wins.")
  private int sourcePriority = 50;

  @Schema(description = "Whether this configured platform may accept translated tasks.")
  private boolean taskable = true;
}
