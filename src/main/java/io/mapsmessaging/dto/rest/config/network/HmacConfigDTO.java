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

package io.mapsmessaging.dto.rest.config.network;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@Schema(description = "HMAC Configuration DTO")
public class HmacConfigDTO extends BaseConfigDTO {

  @Schema(description = "The host for the HMAC configuration", example = "example.com")
  protected String host;

  @Schema(description = "The port used for HMAC communication", example = "8080")
  protected int port;

  @Schema(description = "The secret key for HMAC operations", example = "mySecretKey")
  protected String secret;

  @Schema(description = "The HMAC algorithm to use", example = "HmacSHA256")
  protected String hmacAlgorithm;

  @Schema(description = "The manager handling HMAC operations", example = "Appender")
  protected String hmacManager;

  @Schema(description = "The shared key used for HMAC", example = "sharedKey")
  protected String hmacSharedKey;
}
