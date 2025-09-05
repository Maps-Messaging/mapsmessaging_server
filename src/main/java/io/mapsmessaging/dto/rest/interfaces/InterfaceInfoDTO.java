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

package io.mapsmessaging.dto.rest.interfaces;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper=true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "Interface Information",
    description =
        "Contains details about an interface, including its name, host, port, and current state.")
public class InterfaceInfoDTO extends BaseConfigDTO implements Serializable {

  @Schema(title = "unique id", description = "UUID to reference the interface")
  private String uniqueId;

  @Schema(
      title = "Interface Name",
      description = "Unique name of the interface",
      example = "myInterface")
  private String name;

  @Schema(title = "Port", description = "Port that the interface is bound to", example = "8080")
  private int port;

  @Schema(
      title = "Host",
      description = "Host that the interface is bound to",
      example = "http://localhost")
  private String host;

  @Schema(title = "State", description = "Current state of the interface", example = "Started")
  private String state;

  @Schema(title = "Configuration", description = "Configuration settings for the interface")
  private EndPointServerConfigDTO config;
}
