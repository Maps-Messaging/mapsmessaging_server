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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@Schema(
    title = "SASL Configuration DTO",
    description =
        "Represents the configuration for SASL authentication used for REST communication.")
public class SaslConfigDTO extends BaseConfigDTO {

  @Schema(description = "The realm name used for SASL authentication", example = "example-realm")
  protected String realmName;

  @Schema(description = "The SASL mechanism, such as PLAIN or SCRAM-SHA-256", example = "PLAIN")
  protected String mechanism;

  @Schema(description = "The identity provider for SASL", example = "authProvider123")
  protected String identityProvider;

  @Schema(
      description = "Additional SASL entries as key-value pairs",
      example = "{\"entry1\": \"value1\"}")
  protected Map<String, Object> saslEntries;
}
