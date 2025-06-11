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

package io.mapsmessaging.dto.rest.discovery;

import io.mapsmessaging.network.discovery.services.Services;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "Discovered Servers",
    description =
        "Represents information about discovered servers, including configuration details, schema support, and available services.")
public class DiscoveredServersDTO {

  @Schema(
      title = "Server Name",
      description = "The unique name of the discovered server.",
      example = "myServer",
      nullable = false)
  protected String serverName;

  @Schema(
      title = "System Topic Prefix",
      description = "The name space prefix used for system topics",
      example = "$SYS",
      nullable = true)
  protected String systemTopicPrefix;

  @Schema(
      title = "Schema Support",
      description = "Indicates whether the server supports schema validation for messages.",
      example = "true",
      nullable = false)
  protected boolean schemaSupport;

  @Schema(
      title = "Schema Prefix",
      description = "The name space prefix used for schemas",
      example = "$SCHEMA",
      nullable = true)
  protected String schemaPrefix;

  @Schema(
      title = "Server Version",
      description = "The version of the server software, typically following semantic versioning.",
      example = "1.2.3",
      nullable = false)
  protected String version;

  @Schema(
      title = "Build Date",
      description = "The date the server software was built, formatted as YYYY-MM-DD.",
      example = "2024-01-15",
      nullable = true)
  protected String buildDate;

  @Schema(
      title = "Services",
      description =
          "A map of services provided by the server, where each key is the service name and the value provides service-specific information.",
      example = "{\"mqtt\": {}, \"amqp\": {}}",
      nullable = true)
  protected Map<String, Services> services;
}
