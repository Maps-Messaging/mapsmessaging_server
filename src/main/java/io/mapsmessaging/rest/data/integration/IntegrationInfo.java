/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.rest.data.integration;

import io.mapsmessaging.config.network.EndPointServerConfig;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Data;
import lombok.ToString;

@ToString
@Data
public class IntegrationInfo implements Serializable {

  @Schema(description = "Unique name of the interface")
  private final String name;
  @Schema(description = "Current state of the interface")
  private final String state;
  @Schema(description = "Remote broker url")
  private final String remoteUrl;
  @Schema(description = "Number of mappings")
  private final int mappings;

  @Schema(description = "Configuration for the interface")
  private final EndPointServerConfig config;

  public IntegrationInfo(EndPointConnection endPointConnection){
    name = endPointConnection.getConfigName();
    state = endPointConnection.getState().getName();
    config = endPointConnection.getConfig();
    remoteUrl = endPointConnection.getProperties().getProperty("url", "");
    mappings = endPointConnection.getDestinationMappings().size();
  }

}

