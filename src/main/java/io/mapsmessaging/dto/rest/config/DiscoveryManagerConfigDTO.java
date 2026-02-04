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

package io.mapsmessaging.dto.rest.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(
    description = "Discovery Manager (mDNS) configuration"
)
public class DiscoveryManagerConfigDTO extends BaseManagerConfigDTO {

  @Schema(
      description = "Indicates if the discovery manager is enabled",
      example = "false",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  protected boolean enabled;

  @Schema(
      description = "Comma-separated list of hostnames or IP addresses to bind discovery to. " +
          "Use \"::\" to bind to all interfaces (IPv6 any). Whitespace around commas is ignored.",
      example = "localhost, 192.168.1.10, [2001:db8::1], ::",
      pattern = "^\\s*[^,\\s][^,]*\\s*(?:,\\s*[^,\\s][^,]*\\s*)*$",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  protected String hostnames;

  @Schema(
      description = "Whether to add TXT records to advertised services",
      example = "true",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  protected boolean addTxtRecords;

  @Schema(
      description = "mDNS domain to advertise under. Commonly \"local\" (with or without leading/trailing dot).",
      example = "local",
      pattern = "^(?:\\.)?[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.)?$",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  protected String domainName;

  public DiscoveryManagerConfigDTO() {
    super("DiscoveryManagerConfigDTO");
  }
}