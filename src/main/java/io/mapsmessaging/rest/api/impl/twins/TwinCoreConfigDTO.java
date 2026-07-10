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

package io.mapsmessaging.rest.api.impl.twins;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "TwinCoreConfigDTO",
    description = "Core twin manager timing and lifecycle configuration. Values are persisted to TwinManager configuration and are not applied to the running TwinManager until reload or restart."
)
public class TwinCoreConfigDTO {

  @Schema(
      description = "Time in milliseconds after which a twin is considered disconnected if no updates are received.",
      example = "5000",
      defaultValue = "5000",
      minimum = "1",
      maximum = "600000",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private long heartbeatTimeoutMillis;

  @Schema(
      description = "Time in milliseconds after which a twin is considered stale if no updates are received.",
      example = "10000",
      defaultValue = "10000",
      minimum = "1",
      maximum = "600000",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private long staleTimeoutMillis;

  @Schema(
      description = "Time in milliseconds after which an expired twin may be removed from memory.",
      example = "120000",
      defaultValue = "120000",
      minimum = "0",
      maximum = "86400000",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private long retentionTimeoutMillis;

  @Schema(
      description = "If true, twins that exceed the retention timeout are removed from memory.",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private boolean removeExpiredTwins;

  @Schema(
      description = "Default root path used when constructing twin hierarchical paths.",
      example = "/",
      defaultValue = "/",
      minLength = 1,
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  private String defaultRootPath;

  public long getHeartbeatTimeoutMillis() {
    return heartbeatTimeoutMillis;
  }

  public void setHeartbeatTimeoutMillis(long heartbeatTimeoutMillis) {
    this.heartbeatTimeoutMillis = heartbeatTimeoutMillis;
  }

  public long getStaleTimeoutMillis() {
    return staleTimeoutMillis;
  }

  public void setStaleTimeoutMillis(long staleTimeoutMillis) {
    this.staleTimeoutMillis = staleTimeoutMillis;
  }

  public long getRetentionTimeoutMillis() {
    return retentionTimeoutMillis;
  }

  public void setRetentionTimeoutMillis(long retentionTimeoutMillis) {
    this.retentionTimeoutMillis = retentionTimeoutMillis;
  }

  public boolean isRemoveExpiredTwins() {
    return removeExpiredTwins;
  }

  public void setRemoveExpiredTwins(boolean removeExpiredTwins) {
    this.removeExpiredTwins = removeExpiredTwins;
  }

  public String getDefaultRootPath() {
    return defaultRootPath;
  }

  public void setDefaultRootPath(String defaultRootPath) {
    this.defaultRootPath = defaultRootPath;
  }
}
