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

import io.mapsmessaging.config.routing.PredefinedServerConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Routing Configuration")
public class RoutingManagerConfig extends ManagementConfig{

  private boolean enabled;
  private boolean autoDiscovery;
  private List<PredefinedServerConfig> predefinedServers;

  public static RoutingManagerConfig getInstance(){
    return new RoutingManagerConfig( ConfigurationManager.getInstance().getProperties("routing"));
  }

  private RoutingManagerConfig(ConfigurationProperties properties) {
    enabled = properties.getBooleanProperty("enabled", false);
    autoDiscovery = properties.getBooleanProperty("autoDiscovery", true);
    predefinedServers = new ArrayList<>();
    Object servers = properties.get("predefinedServers");
    if (servers instanceof List) {
      for (ConfigurationProperties serverProps : (List<ConfigurationProperties>) servers) {
        predefinedServers.add(new PredefinedServerConfig(serverProps));
      }
    }
  }

  @Override
  public boolean update(ManagementConfig config) {
    return false;
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("enabled", enabled);
    properties.put("autoDiscovery", autoDiscovery);
    List<ConfigurationProperties> serverPropertiesList = new ArrayList<>();
    for (PredefinedServerConfig server : predefinedServers) {
      serverPropertiesList.add(server.toConfigurationProperties());
    }
    properties.put("predefinedServers", serverPropertiesList);
    return properties;
  }
}
