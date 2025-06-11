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

package io.mapsmessaging.config;

import io.mapsmessaging.config.tenant.TenantConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.TenantManagementConfigDTO;
import io.mapsmessaging.dto.rest.config.tenant.TenantConfigDTO;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class TenantManagementConfig extends TenantManagementConfigDTO implements Config, ConfigManager {

  public static TenantManagementConfig getInstance() {
    return ConfigurationManager.getInstance().getConfiguration(TenantManagementConfig.class);
  }

  private TenantManagementConfig(ConfigurationProperties properties) {
    tenantConfigList = new ArrayList<>();
    Object configEntry = properties.get("data");
    if (configEntry instanceof List) {
      for (ConfigurationProperties entry : (List<ConfigurationProperties>) configEntry) {
        tenantConfigList.add(new TenantConfig(entry));
      }
    } else if (configEntry instanceof ConfigurationProperties) {
      tenantConfigList.add(new TenantConfig((ConfigurationProperties) configEntry));
    }
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof TenantManagementConfigDTO)) {
      return false;
    }

    TenantManagementConfigDTO newConfig = (TenantManagementConfigDTO) config;
    boolean hasChanged = false;

    if (this.tenantConfigList.size() != newConfig.getTenantConfigList().size()) {
      this.tenantConfigList = newConfig.getTenantConfigList();
      hasChanged = true;
    } else {
      for (int i = 0; i < this.tenantConfigList.size(); i++) {
        if (!this.tenantConfigList.get(i).equals(newConfig.getTenantConfigList().get(i))) {
          this.tenantConfigList.set(i, newConfig.getTenantConfigList().get(i));
          hasChanged = true;
        }
      }
    }

    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties configurationProperties = new ConfigurationProperties();
    List<ConfigurationProperties> tenantList = new ArrayList<>();
    for (TenantConfigDTO tenantConfig : tenantConfigList) {
      tenantList.add(((Config)tenantConfig).toConfigurationProperties());
    }
    configurationProperties.put("data", tenantList);
    return configurationProperties;
  }

  @Override
  public ConfigManager load(FeatureManager featureManager) {
    return new TenantManagementConfig(ConfigurationManager.getInstance().getProperties(getName()));
  }

  @Override
  public void save() throws IOException {
    ConfigurationManager.getInstance().saveConfiguration(getName(), toConfigurationProperties());
  }


  @Override
  public String getName() {
    return "TenantManagement";
  }
}
