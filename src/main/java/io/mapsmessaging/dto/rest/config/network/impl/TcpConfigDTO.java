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

package io.mapsmessaging.dto.rest.config.network.impl;

import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(
    description = "TCP Configuration DTO",
    additionalProperties = Schema.AdditionalPropertiesValue.TRUE,
    additionalPropertiesSchema = Object.class
)
public class TcpConfigDTO extends EndPointConfigDTO {

  public TcpConfigDTO() {
    this("tcp");
  }

  protected TcpConfigDTO(String type) {
    super(type);
  }

  @Schema(
      description = "Size of the receive buffer (bytes)",
      example = "128000",
      defaultValue = "128000",
      minimum = "1024",
      maximum = "104857600",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected int receiveBufferSize = 128000;

  @Schema(
      description = "Size of the send buffer (bytes)",
      example = "128000",
      defaultValue = "128000",
      minimum = "1024",
      maximum = "104857600",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected int sendBufferSize = 128000;

  @Schema(
      description = "Connection timeout in milliseconds",
      example = "60000",
      defaultValue = "60000",
      minimum = "1",
      maximum = "3600000",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected int timeout = 60000;

  @Schema(
      description = "Backlog for TCP connections",
      example = "100",
      defaultValue = "100",
      minimum = "10",
      maximum = "10000",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected int backlog = 100;

  @Schema(
      description = "SO_LINGER delay in seconds (0 disables linger)",
      example = "10",
      defaultValue = "10",
      minimum = "0",
      maximum = "60",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected int soLingerDelaySec = 10;

  @Schema(
      description = "Read delay in milliseconds when fragmentation is detected",
      example = "100",
      defaultValue = "100",
      minimum = "1",
      maximum = "1000",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected int readDelayOnFragmentation = 100;

  @Schema(
      description = "Maximum allowed fragmentation before applying backoff logic",
      example = "5",
      defaultValue = "5",
      minimum = "2",
      maximum = "100",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected int fragmentationLimit = 5;

  @Schema(
      description = "Enable read delay on fragmentation",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected boolean enableReadDelayOnFragmentation = true;
}

