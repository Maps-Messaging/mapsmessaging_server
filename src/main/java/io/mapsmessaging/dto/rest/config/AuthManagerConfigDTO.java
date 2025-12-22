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

package io.mapsmessaging.dto.rest.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "Auth Manager Configuration DTO")
public class AuthManagerConfigDTO extends BaseConfigDTO {

  @Schema(description = "Indicates if authentication is enabled", example = "true")
  protected boolean authenticationEnabled;

  @Schema(description = "Indicates if authorization is enabled", example = "true")
  protected boolean authorisationEnabled;

  @Schema(description = "Configuration properties for authentication", implementation = Map.class)
  protected Map<String, Object> authConfig;

  @Schema(
      description = "Number of consecutive authentication failures required before an account is locked.",
      example = "5",
      minimum = "1"
  )
  protected int maxFailuresBeforeLock = 5;

  @Schema(
      description = "Initial lock duration in seconds once the failure threshold is exceeded. Subsequent locks may increase up to the configured maximum.",
      example = "30",
      minimum = "1"
  )
  protected int initialLockSeconds = 30;

  @Schema(
      description = "Maximum lock duration in seconds. Lock times will not grow beyond this value regardless of repeated failures.",
      example = "900",
      minimum = "1"
  )
  protected int maxLockSeconds = 900;

  @Schema(
      description = "Time in seconds after which recorded authentication failures decay if no new failures occur. This allows accounts to recover naturally over time.",
      example = "900",
      minimum = "1"
  )
  protected int failureDecaySeconds = 900;

  @Schema(
      description = "Enable progressive response delays before lockout is triggered. When enabled, each failed attempt adds a short delay before authentication is processed.",
      example = "true"
  )
  protected boolean enableSoftDelay = true;

  @Schema(
      description = "Additional delay in milliseconds applied per authentication failure when soft delay is enabled.",
      example = "200",
      minimum = "0"
  )
  protected int softDelayMillisPerFailure = 200;

  @Schema(
      description = "Maximum cumulative soft delay in milliseconds that can be applied before authentication processing. Prevents unbounded delays.",
      example = "2000",
      minimum = "0"
  )
  protected int maxSoftDelayMillis = 2000;


}
