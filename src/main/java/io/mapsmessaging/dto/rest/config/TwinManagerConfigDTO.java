
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
package io.mapsmessaging.dto.rest.config;

import io.mapsmessaging.dto.rest.config.protocol.impl.TakProtocolDTO;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "State/Twin Manager Configuration DTO")
public class TwinManagerConfigDTO extends BaseManagerConfigDTO {

  @Schema(
      description = "Time in milliseconds after which a twin is considered disconnected if no updates are received.",
      example = "5000",
      defaultValue = "5000",
      minimum = "1",
      maximum = "600000",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected long heartbeatTimeoutMillis = 5000L;

  @Schema(
      description = "Time in milliseconds after which a twin is considered stale if no updates are received.",
      example = "10000",
      defaultValue = "10000",
      minimum = "1",
      maximum = "600000",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected long staleTimeoutMillis = 10000L;

  @Schema(
      description = "Time in milliseconds after which a twin is eligible for removal from memory.",
      example = "300000",
      defaultValue = "300000",
      minimum = "0",
      maximum = "86400000",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected long retentionTimeoutMillis = 120000L;

  @Schema(
      description = "If true, twins that exceed the retention timeout will be removed from memory.",
      example = "false",
      defaultValue = "false",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected boolean removeExpiredTwins = true;

  @Schema(
      description = "Default root path used when constructing twin hierarchical paths.",
      example = "/",
      defaultValue = "/",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected String defaultRootPath = "/";

  @Schema(
      description = "Optional TAK protocol configuration used to publish twin updates to a TAK server.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected TakProtocolDTO tak;

  public TwinManagerConfigDTO() {
    super("TwinManagerConfigDTO");
  }

  @Override
  public String getSimpleName() {
    return "State Cache";
  }
}