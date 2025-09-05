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
@Schema(description = "AMQP Protocol Configuration DTO")
public class AmqpConfigDTO extends ProtocolConfigDTO {

  @Schema(description = "Idle timeout in milliseconds", example = "30000")
  protected int idleTimeout = 30000;

  @Schema(description = "Maximum frame size in bytes", example = "65536")
  protected int maxFrameSize = 65536;

  @Schema(description = "Link credit for the AMQP connection", example = "50")
  protected int linkCredit = 50;

  @Schema(description = "Specifies if the AMQP link is durable", example = "false")
  protected boolean durable = false;

  @Schema(description = "Incoming capacity of the AMQP session", example = "65536")
  protected int incomingCapacity = 65536;

  @Schema(description = "Outgoing window size for the AMQP session", example = "100")
  protected int outgoingWindow = 100;
}
