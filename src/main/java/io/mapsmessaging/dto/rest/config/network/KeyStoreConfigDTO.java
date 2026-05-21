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

package io.mapsmessaging.dto.rest.config.network;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@Schema(description = "Key Store Configuration DTO")
public class KeyStoreConfigDTO extends BaseConfigDTO {

  @Schema(
      description = "Alias used in the key store. If not set, the first suitable key entry may be used.",
      example = "myKeyAlias",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String alias;

  @Schema(
      description = "Type of the key store",
      example = "PKCS12",
      defaultValue = "PKCS12",
      allowableValues = {"JKS", "PKCS11", "PKCS12", "JCEKS", "BKS", "UBER", "BCFKS"},
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected String type = "PKCS12";

  @Schema(
      description = "Security provider name used for KeyStore/SSL operations (optional). " +
          "Examples: SunJSSE, SUN, SunRsaSign, BC (BouncyCastle).",
      example = "SunJSSE",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String providerName;

  @Schema(
      description = "KeyManagerFactory algorithm (optional). Common values: SunX509, NewSunX509, PKIX.",
      example = "SunX509",
      defaultValue = "SunX509",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String managerFactory = "SunX509";

  @Schema(
      description = "Path to the key store file. Not required for PKCS11 (which is typically configured via provider settings).",
      example = "/path/to/keystore.p12",
      minLength = 1,
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String path;

  @Schema(
      description = "Passphrase for the key store. Optional depending on key store type and provider.",
      example = "changeit",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String passphrase;

  @Schema(
      description = "Provider identifier used to load the KeyStore (optional). " +
          "If both providerName and provider are set, providerName typically takes precedence.",
      example = "SunJSSE",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String provider;
}
