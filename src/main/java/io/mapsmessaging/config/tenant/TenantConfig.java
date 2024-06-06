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

package io.mapsmessaging.config.tenant;


import io.mapsmessaging.configuration.ConfigurationProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Tenant Configuration")
public class TenantConfig {

  private String name;
  private String namespaceRoot;
  private String scope;

  public TenantConfig(ConfigurationProperties properties) {
    name = properties.getProperty("name", "");
    namespaceRoot = properties.getProperty("namespaceRoot", "");
    scope = properties.getProperty("scope", "");
  }

  public boolean update(TenantConfig newConfig) {
    boolean hasChanged = false;

    if (!this.name.equals(newConfig.getName())) {
      this.name = newConfig.getName();
      hasChanged = true;
    }
    if (!this.namespaceRoot.equals(newConfig.getNamespaceRoot())) {
      this.namespaceRoot = newConfig.getNamespaceRoot();
      hasChanged = true;
    }
    if (!this.scope.equals(newConfig.getScope())) {
      this.scope = newConfig.getScope();
      hasChanged = true;
    }

    return hasChanged;
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", name);
    properties.put("namespaceRoot", namespaceRoot);
    properties.put("scope", scope);
    return properties;
  }
}

