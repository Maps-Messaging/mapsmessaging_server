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
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "SSL/TLS Configuration DTO")
public class SslConfigDTO extends BaseConfigDTO {

  @Schema(
      description = "Whether a client certificate is required for connections. " +
          "If true, connections without a valid client certificate will be rejected.",
      example = "false",
      defaultValue = "false",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected boolean clientCertificateRequired = false;

  @Schema(
      description = "Whether a client certificate is requested but not required. " +
          "Ignored if clientCertificateRequired is true.",
      example = "false",
      defaultValue = "false",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected boolean clientCertificateWanted = false;

  @Schema(
      description = "URL for the Certificate Revocation List (CRL). " +
          "If not set, CRL checking is disabled.",
      example = "http://example.com/crl.pem",
      format = "uri",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String crlUrl;

  @Schema(
      description = "Interval in milliseconds for refreshing the Certificate Revocation List (CRL)",
      example = "3600000",
      defaultValue = "3600000",
      minimum = "60000",
      maximum = "2419200000",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected long crlInterval = 3600000L;

  @Schema(
      description = "SSL context identifier or protocol profile to use (for example: TLS, TLSv1.2, TLSv1.3).",
      example = "TLS",
      defaultValue = "TLS",
      pattern = "^TLS(?:v1\\.(?:2|3))?$",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected String context = "TLS";

  @Schema(
      description = "Key store configuration containing the server certificate and private key",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected KeyStoreConfigDTO keyStore;

  @Schema(
      description = "Trust store configuration containing trusted Certificate Authorities",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      example = "{\"type\":\"PKCS12\",\"path\":\"/etc/maps/trust.p12\",\"passphrase\":\"changeit\"}"
  )
  protected KeyStoreConfigDTO trustStore;
}
