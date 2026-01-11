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

import io.mapsmessaging.config.network.impl.TlsConfig;
import io.mapsmessaging.config.rest.StaticConfig;
import io.mapsmessaging.dto.rest.config.network.impl.TlsConfigDTO;
import io.mapsmessaging.dto.rest.config.rest.StaticConfigDTO;
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

  @Schema(
      description = "Indicates if REST API is enabled",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected boolean enabled = true;

  @Schema(
      description = "Enables authentication for REST API endpoints",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected boolean enableAuthentication = true;

  @Schema(
      description = "Comma-separated list of hostnames or IP addresses to bind to. Whitespace around commas is ignored.",
      example = "0.0.0.0, ::",
      defaultValue = "0.0.0.0",
      pattern = "^\\s*[^,\\s][^,]*\\s*(?:,\\s*[^,\\s][^,]*\\s*)*$",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected String hostnames = "0.0.0.0";

  @Schema(
      description = "Port for the REST API",
      example = "8080",
      defaultValue = "8080",
      minimum = "1",
      maximum = "65535",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected int port = 8080;

  @Schema(
      description = "Enable caching of REST responses",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected boolean enableCache = true;

  @Schema(
      description = "Minimum number of network threads",
      example = "2",
      defaultValue = "2",
      minimum = "1",
      maximum = "1000",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected int minThreads = 2;

  @Schema(
      description = "Maximum number of network threads",
      example = "5",
      defaultValue = "5",
      minimum = "1",
      maximum = "1000",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected int maxThreads = 5;

  @Schema(
      description = "Thread queue limit (maximum queued tasks)",
      example = "100",
      defaultValue = "100",
      minimum = "10",
      maximum = "1000",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected int threadQueueLimit = 100;

  @Schema(
      description = "Selector thread count",
      example = "2",
      defaultValue = "2",
      minimum = "1",
      maximum = "1000",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected int selectorThreads = 2;

  @Schema(
      description = "Maximum outstanding events per destination",
      example = "10",
      defaultValue = "10",
      minimum = "1",
      maximum = "1000",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected int maxEventsPerDestination = 10;

  @Schema(
      description = "Cache element lifetime in milliseconds",
      example = "60000",
      defaultValue = "60000",
      minimum = "1000",
      maximum = "600000",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected long cacheLifetime = 60000L;

  @Schema(
      description = "Cache cleanup interval in milliseconds",
      example = "5000",
      defaultValue = "5000",
      minimum = "1000",
      maximum = "600000",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected long cacheCleanup = 5000L;

  @Schema(
      description = "Session inactive timeout in milliseconds",
      example = "180000",
      defaultValue = "180000",
      minimum = "60000",
      maximum = "600000",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected int inactiveTimeout = 180000;

  @Schema(
      description = "If set, enables the /application.wadl endpoint",
      example = "false",
      defaultValue = "false",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected boolean enableWadlEndPoint = false;

  @Schema(
      description = "Enables Swagger/OpenAPI documentation endpoints",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected boolean enableSwagger = true;

  @Schema(
      description = "Enables Swagger UI",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected boolean enableSwaggerUI = true;

  @Schema(
      description = "Enables User Management features",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected boolean enableUserManagement = true;

  @Schema(
      description = "Enables Schema Management features",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected boolean enableSchemaManagement = true;

  @Schema(
      description = "Enables Interface Management features",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected boolean enableInterfaceManagement = true;

  @Schema(
      description = "Enables Destination Management features",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected boolean enableDestinationManagement = true;

  @Schema(
      description = "TLS configuration",
      implementation = TlsConfig.class,
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected TlsConfigDTO tlsConfig;

  @Schema(
      description = "Static content configuration",
      implementation = StaticConfig.class,
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected StaticConfigDTO staticConfig;

  @Schema(
      description = "CORS configuration",
      implementation = CorsHeaders.class,
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected CorsHeaders corsHeaders;
}
