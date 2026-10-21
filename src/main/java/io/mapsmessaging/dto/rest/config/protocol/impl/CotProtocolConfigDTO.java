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

package io.mapsmessaging.dto.rest.config.protocol.impl;

import io.mapsmessaging.dto.rest.config.network.KeyStoreConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Raw CoT XML passthrough ingest protocol Configuration DTO")
public class CotProtocolConfigDTO extends ProtocolConfigDTO {

  public CotProtocolConfigDTO() {
    super("cot");
  }

  @Schema(description = "Hostname of the real TAK server each received CoT event is forwarded to", example = "tak.example.org")
  protected String takHostname = "";

  @Schema(description = "Port of the real TAK server each received CoT event is forwarded to", example = "8088")
  protected int takPort = 8088;

  @Schema(description = "If true, the connection to the TAK server uses TLS instead of plain TCP", example = "false")
  protected boolean takTlsEnabled = false;

  @Schema(description = "TLS protocol used for the TAK server connection when takTlsEnabled is true", example = "TLSv1.2")
  protected String takTlsContext = "TLSv1.2";

  @Schema(description = "Key store presenting this broker's client certificate to the TAK server (mutual TLS). Required when takTlsEnabled is true.")
  protected KeyStoreConfigDTO takKeyStore;

  @Schema(description = "Trust store used to validate the TAK server's certificate. Required when takTlsEnabled is true.")
  protected KeyStoreConfigDTO takTrustStore;
}
