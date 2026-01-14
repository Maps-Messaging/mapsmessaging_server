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

package io.mapsmessaging.dto.rest.auth;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.ConfigurationManagerDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "Security Manager",
    description = "Mapping between auth config names and JAAS configuration names"
)
public class SecurityManagerDTO extends BaseConfigDTO implements ConfigurationManagerDTO {

  @Schema(
      title = "Mapping",
      description = "Map of auth configuration name (key) to JAAS configuration name (value). " +
          "If authName is blank, the value for key \"default\" is used.",
      type = "object",
      additionalPropertiesSchema = String.class,
      example = "{\"default\":\"PublicAuthConfig\",\"Default\":\"PublicAuthConfig\"}",
      nullable = false,
      requiredMode = Schema.RequiredMode.REQUIRED,
      additionalProperties = Schema.AdditionalPropertiesValue.TRUE
  )
  protected Map<String, String> map;

  public String getAuthName(String authName) {
    if (authName != null && !authName.isEmpty()) {
      return map.get(authName);
    } else {
      return map.get("default");
    }
  }
}
