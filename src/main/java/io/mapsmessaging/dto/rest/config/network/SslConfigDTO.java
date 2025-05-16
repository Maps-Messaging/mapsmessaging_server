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
@Schema(description = "SSL Configuration DTO")
public class SslConfigDTO extends BaseConfigDTO {

  @Schema(description = "Whether client certificate is required", example = "true")
  protected boolean clientCertificateRequired;

  @Schema(description = "Whether client certificate is wanted", example = "true")
  protected boolean clientCertificateWanted;

  @Schema(description = "URL for Certificate Revocation List", example = "http://example.com/crl")
  protected String crlUrl;

  @Schema(description = "Interval in milliseconds for CRL refresh", example = "3600000")
  protected long crlInterval;

  @Schema(description = "SSL context identifier", example = "TLSv3")
  protected String context;

  @Schema(description = "Key store configuration")
  protected KeyStoreConfigDTO keyStore;

  @Schema(description = "Trust store configuration")
  protected KeyStoreConfigDTO trustStore;
}

