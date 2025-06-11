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

package io.mapsmessaging.dto.rest.config.auth;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "Auth Configuration DTO",
    description = "Represents authentication configuration settings for REST communication.")
public class AuthConfigDTO extends BaseConfigDTO {

  @Schema(description = "Username for authentication", example = "user123")
  protected String username;

  @Schema(description = "Password for authentication", example = "password")
  protected String password;

  @Schema(description = "Session ID for the authentication session", example = "session-xyz")
  protected String sessionId;

  @Schema(description = "Token generator type", example = "JWT")
  protected String tokenGenerator;

  @Schema(
      description = "Configuration settings for the token generator",
      example = "{\"expiry\": 3600}")
  protected Map<String, Object> tokenConfig;
}
