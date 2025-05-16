/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "Semtech Protocol Configuration DTO")
public class SemtechConfigDTO extends ProtocolConfigDTO {

  @Schema(description = "Maximum queue size for Semtech", example = "10")
  protected int maxQueued = 10;

  @Schema(description = "Inbound topic name for Semtech messages", example = "/semtech/inbound")
  protected String inboundTopicName = "/semtech/inbound";

  @Schema(description = "Outbound topic name for Semtech messages", example = "/semtech/outbound")
  protected String outboundTopicName = "/semtech/outbound";

  @Schema(description = "Status topic name for Semtech", example = "/semtech/status")
  protected String statusTopicName = "/semtech/status";
}
