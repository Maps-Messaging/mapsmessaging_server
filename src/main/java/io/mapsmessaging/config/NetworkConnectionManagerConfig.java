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

import io.mapsmessaging.config.network.EndPointConnectionServerConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.NetworkConnectionManagerConfigDTO;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class NetworkConnectionManagerConfig extends NetworkConnectionManagerConfigDTO implements Config, ConfigManager {

  private NetworkConnectionManagerConfig(ConfigurationProperties config) {
    ConfigurationProperties globalConfig = config.getGlobal();
    endPointServerConfigList = new ArrayList<>();
    Object obj = config.get("data");
    if (obj instanceof List) {
      for (ConfigurationProperties entry : (List<ConfigurationProperties>) obj) {
        endPointServerConfigList.add(new EndPointConnectionServerConfig(entry));
      }
    } else if (obj instanceof ConfigurationProperties) {
      endPointServerConfigList.add(new EndPointConnectionServerConfig((ConfigurationProperties) obj));
    }
  }

  public static NetworkConnectionManagerConfig getInstance() {
    return ConfigurationManager.getInstance().getConfiguration(NetworkConnectionManagerConfig.class);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof NetworkConnectionManagerConfigDTO newConfig) {
      if (this.endPointServerConfigList.size() != newConfig.getEndPointServerConfigList().size()) {
        this.endPointServerConfigList = newConfig.getEndPointServerConfigList();
        hasChanged = true;
      } else {
        for (int i = 0; i < this.endPointServerConfigList.size(); i++) {
          if (!this.endPointServerConfigList.get(i).equals(newConfig.getEndPointServerConfigList().get(i))) {
            this.endPointServerConfigList.set(i, newConfig.getEndPointServerConfigList().get(i));
            hasChanged = true;
          }
        }
      }
    }
    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    List<ConfigurationProperties> data = new ArrayList<>();
    for (EndPointConnectionServerConfig endPointServerConfig : endPointServerConfigList) {
      data.add(endPointServerConfig.toConfigurationProperties());
    }
    config.put("data", data);
    return config;
  }

  @Override
  public ConfigManager load(FeatureManager featureManager) {
    return new NetworkConnectionManagerConfig(ConfigurationManager.getInstance().getProperties(getName()));  }

  @Override
  public void save() throws IOException {
    ConfigurationManager.getInstance().saveConfiguration(getName(), toConfigurationProperties());
  }


  @Override
  public String getName() {
    return "NetworkConnectionManager";
  }
}
