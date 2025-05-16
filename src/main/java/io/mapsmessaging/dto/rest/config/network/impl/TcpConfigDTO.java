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

package io.mapsmessaging.dto.rest.config.network.impl;

import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper=true)
@NoArgsConstructor
@Schema(description = "TCP Configuration DTO")
public class TcpConfigDTO extends EndPointConfigDTO {

  @Schema(description = "Size of the receive buffer", example = "128000")
  protected int receiveBufferSize;

  @Schema(description = "Size of the send buffer", example = "128000")
  protected int sendBufferSize;

  @Schema(description = "Connection timeout in milliseconds", example = "60000")
  protected int timeout;

  @Schema(description = "Backlog for TCP connections", example = "100")
  protected int backlog;

  @Schema(description = "SO linger delay in seconds", example = "10")
  protected int soLingerDelaySec;

  @Schema(description = "Read delay on fragmentation", example = "100")
  protected int readDelayOnFragmentation;

  @Schema(description = "Fragmentation limit for the connection", example = "5")
  protected int fragmentationLimit;

  @Schema(description = "Enable read delay on fragmentation", example = "true")
  protected boolean enableReadDelayOnFragmentation;
}
