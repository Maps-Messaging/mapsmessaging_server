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
@Schema(description = "Key Store Configuration DTO")
public class KeyStoreConfigDTO extends BaseConfigDTO {

  @Schema(description = "Alias used in the key store", example = "myKeyAlias")
  protected String alias;

  @Schema(description = "Type of the key store", example = "JKS")
  protected String type;

  @Schema(description = "Name of the security provider", example = "SunJSSE")
  protected String providerName;

  @Schema(description = "Key manager factory algorithm", example = "SunX509")
  protected String managerFactory;

  @Schema(description = "Path to the key store file", example = "/path/to/keystore.jks")
  protected String path;

  @Schema(description = "Passphrase for the key store", example = "changeit")
  protected String passphrase;

  @Schema(description = "Provider name for the key store", example = "SunJSSE")
  protected String provider;

}
