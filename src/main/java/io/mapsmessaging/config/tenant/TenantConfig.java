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

package io.mapsmessaging.config.tenant;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.tenant.TenantConfigDTO;

public class TenantConfig extends TenantConfigDTO implements Config {

  public TenantConfig(ConfigurationProperties properties) {
    super();
    this.name = properties.getProperty("name", "");
    this.namespaceRoot = properties.getProperty("namespaceRoot", "");
    this.scope = properties.getProperty("scope", "");
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", this.name);
    properties.put("namespaceRoot", this.namespaceRoot);
    properties.put("scope", this.scope);
    return properties;
  }

  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (!(config instanceof TenantConfigDTO)) {
      return false;
    }

    TenantConfigDTO newConfig = (TenantConfigDTO) config;
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
}
