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
@Schema(description = "NATS Protocol Configuration DTO")
public class NatsConfigDTO extends ProtocolConfigDTO {

  @Schema(description = "Maximum buffer size for NATS", example = "65535")
  protected int maxBufferSize = 65535;

  @Schema(description = "Maximum receive limit for NATS", example = "1000")
  protected int maxReceive = 1000;

  @Schema(description = "Enable NATS Streams via jetstream", example = "true")
  protected boolean enableStreams = false;

  @Schema(description = "Enable NATS Key values via jetstream", example = "true")
  protected boolean enableKeyValues = false;

  @Schema(description = "Enable NATS object store via jetstream", example = "true")
  protected boolean enableObjectStore = false;

  @Schema(description = "Ping timeout in milliseconds", example = "60000")
  protected int keepAlive = 60_000;

  @Schema(description = "Root for the NATS streams", example = "/nats", defaultValue = "")
  protected String namespaceRoot = "";

  @Schema(description = "Enable or disable stream deletion", example = "true")
  protected boolean enableStreamDelete = true;

}
