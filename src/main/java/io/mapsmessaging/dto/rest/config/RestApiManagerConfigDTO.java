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

package io.mapsmessaging.dto.rest.config;

import io.mapsmessaging.config.network.impl.TlsConfig;
import io.mapsmessaging.config.rest.StaticConfig;
import io.mapsmessaging.rest.handler.CorsHeaders;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "Rest API Configuration DTO")
public class RestApiManagerConfigDTO extends BaseConfigDTO {

  @Schema(description = "Indicates if REST API is enabled", example = "true")
  protected boolean enabled;

  @Schema(description = "Enables authentication", example = "true")
  protected boolean enableAuthentication;

  @Schema(description = "Hostnames for binding", example = "0.0.0.0")
  protected String hostnames;

  @Schema(description = "Port for the REST API", example = "8080")
  protected int port;

  @Schema(description = "Enable Caching of rest responses", example = "true")
  protected boolean enableCache;

  @Schema(description = "Minimum number of network threads", example = "10")
  protected int minThreads;

  @Schema(description = "Maximum number of network threads", example = "50")
  protected int maxThreads;

  @Schema(description = "Thread queue limit", example = "100")
  protected int threadQueueLimit;

  @Schema(description = "Selector threads", example = "2")
  protected int selectorThreads;

  @Schema(description = "Max outstanding events per destination", example="10")
  protected int maxEventsPerDestination;

  @Schema(description = "Cache element life time in ms", example = "10000")
  protected long cacheLifetime;

  @Schema(description = "Cache element cleanup time in ms", example = "5000")
  protected long cacheCleanup;

  @Schema(description = "Session inactive timeout in ms", example = "180000")
  protected int inactiveTimeout;

  @Schema(description = "If set, enables the /application.wadl end point", example = "false")
  protected boolean enableWadlEndPoint;

  @Schema(description = "Enables Swagger documentation", example = "true")
  protected boolean enableSwagger;

  @Schema(description = "Enables Swagger UI", example = "true")
  protected boolean enableSwaggerUI;

  @Schema(description = "Enables User Management features", example = "true")
  protected boolean enableUserManagement;

  @Schema(description = "Enables Schema Management features", example = "true")
  protected boolean enableSchemaManagement;

  @Schema(description = "Enables Interface Management features", example = "true")
  protected boolean enableInterfaceManagement;

  @Schema(description = "Enables Destination Management features", example = "true")
  protected boolean enableDestinationManagement;

  @Schema(description = "TLS configuration", implementation = TlsConfig.class)
  protected TlsConfig tlsConfig;

  @Schema(description = "Static configuration", implementation = StaticConfig.class)
  protected StaticConfig staticConfig;

  @Schema(description = "CORS headers", implementation = CorsHeaders.class)
  protected CorsHeaders corsHeaders;

}
