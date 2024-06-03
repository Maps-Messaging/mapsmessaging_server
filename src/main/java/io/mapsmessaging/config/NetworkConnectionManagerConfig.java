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

package io.mapsmessaging.config;

import io.mapsmessaging.config.network.EndPointConnectionServerConfig;
import io.mapsmessaging.config.network.EndPointServerConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
@Schema(description = "Network Connection Manager Configuration")
public class NetworkConnectionManagerConfig extends Config {

  private EndPointServerConfig global;
  private List<EndPointConnectionServerConfig> endPointServerConfigList;

  private NetworkConnectionManagerConfig(ConfigurationProperties config) {
    ConfigurationProperties globalConfig = config.getGlobal();
    if (globalConfig != null) {
      this.global = new EndPointServerConfig(globalConfig);
    }
    Object obj = config.get("data");
    endPointServerConfigList = new ArrayList<>();
    if (obj instanceof List) {
      for (ConfigurationProperties entry : (List<ConfigurationProperties>) obj) {
        endPointServerConfigList.add(new EndPointConnectionServerConfig(entry));
      }
    } else if (obj instanceof ConfigurationProperties) {
      endPointServerConfigList.add(new EndPointConnectionServerConfig((ConfigurationProperties) obj));
    }
  }

  public static NetworkConnectionManagerConfig getInstance() {
    return new NetworkConnectionManagerConfig (ConfigurationManager.getInstance().getProperties("NetworkConnectionManager"));
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    if (global != null) {
      config.put("global", this.global.toConfigurationProperties());
    }
    List<ConfigurationProperties> data = new ArrayList<>();
    for (EndPointConnectionServerConfig endPointServerConfig : endPointServerConfigList) {
      data.add(endPointServerConfig.toConfigurationProperties());
    }
    config.put("data", data);
    return config;
  }
}
