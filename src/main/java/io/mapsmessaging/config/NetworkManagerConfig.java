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

import io.mapsmessaging.config.network.EndPointServerConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.NetworkManagerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
public class NetworkManagerConfig extends NetworkManagerConfigDTO implements Config, ConfigManager {

  private NetworkManagerConfig(ConfigurationProperties config) {
    this.preferIpV6Addresses = config.getBooleanProperty("preferIPv6Addresses", true);
    this.scanNetworkChanges = config.getBooleanProperty("scanNetworkChanges", true);
    this.scanInterval = config.getIntProperty("scanInterval", 60000);

    ConfigurationProperties globalConfig = config.getGlobal();

    this.endPointServerConfigList = new ArrayList<>();
    Object obj = config.get("data");
    if (obj instanceof List) {
      for (ConfigurationProperties entry : (List<ConfigurationProperties>) obj) {
        this.endPointServerConfigList.add(new EndPointServerConfig(entry));
      }
    } else if (obj instanceof ConfigurationProperties) {
      this.endPointServerConfigList.add(new EndPointServerConfig((ConfigurationProperties) obj));
    }
  }

  public static NetworkManagerConfig getInstance() {
    return  ConfigurationManager.getInstance().getConfiguration(NetworkManagerConfig.class);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if(config instanceof EndPointServerConfigDTO) {
      return updateEndPointServerConfig((EndPointServerConfigDTO) config);
    }

    if (!(config instanceof NetworkManagerConfigDTO)) {
      return false;
    }


    NetworkManagerConfigDTO newConfig = (NetworkManagerConfigDTO) config;
    boolean hasChanged = false;

    if (this.preferIpV6Addresses != newConfig.isPreferIpV6Addresses()) {
      this.preferIpV6Addresses = newConfig.isPreferIpV6Addresses();
      hasChanged = true;
    }
    if (this.scanNetworkChanges != newConfig.isScanNetworkChanges()) {
      this.scanNetworkChanges = newConfig.isScanNetworkChanges();
      hasChanged = true;
    }
    if (this.scanInterval != newConfig.getScanInterval()) {
      this.scanInterval = newConfig.getScanInterval();
      hasChanged = true;
    }
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

    return hasChanged;
  }

  private boolean updateEndPointServerConfig(EndPointServerConfigDTO endPointServerConfig) {
    for(EndPointServerConfigDTO endPointServerConfigDTO : this.endPointServerConfigList) {
      if(endPointServerConfigDTO.getName().equals(endPointServerConfig.getName()) && endPointServerConfigDTO instanceof EndPointServerConfig) {
        return ((EndPointServerConfig) endPointServerConfig).update(endPointServerConfigDTO);
      }
    }
    return false;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    List<Map<String, Object>> data = new ArrayList<>();
    for (EndPointServerConfigDTO endPointServerConfig : this.endPointServerConfigList) {
      ConfigurationProperties endPoint = ((Config)endPointServerConfig).toConfigurationProperties();
      data.add(endPoint.getMap());
    }

    config.put("data", data);
    config.put("preferIPv6Addresses", this.preferIpV6Addresses);
    config.put("scanInterval", this.scanInterval);
    config.put("scanNetworkChanges", this.scanNetworkChanges);

    return config;
  }

  @Override
  public ConfigManager load(FeatureManager featureManager) {
    return new NetworkManagerConfig(ConfigurationManager.getInstance().getProperties(getName()));
  }

  @Override
  public void save() throws IOException {
    ConfigurationManager.getInstance().saveConfiguration(getName(), toConfigurationProperties());
  }


  @Override
  public String getName() {
    return "NetworkManager";
  }
}
