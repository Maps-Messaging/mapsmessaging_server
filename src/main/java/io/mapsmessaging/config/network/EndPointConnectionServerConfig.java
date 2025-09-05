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

package io.mapsmessaging.config.network;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.config.auth.AuthConfig;
import io.mapsmessaging.config.protocol.LinkConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointConnectionServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;

import java.util.ArrayList;
import java.util.List;

public class EndPointConnectionServerConfig extends EndPointConnectionServerConfigDTO implements Config {

  public EndPointConnectionServerConfig(ConfigurationProperties props) {
    EndPointConfigFactory.unpack(props, this);
    this.authConfig = new AuthConfig(props);
    this.linkTransformation = props.getProperty("transformation", "");
    this.pluginConnection = props.getBooleanProperty("plugin", false);
    this.cost = props.getIntProperty("cost", 0);
    this.groupName = props.getProperty("groupName", "");

    linkConfigs = new ArrayList<>();
    Object obj = props.get("links");
    if (obj instanceof List) {
      for (ConfigurationProperties entry : (List<ConfigurationProperties>) obj) {
        linkConfigs.add(new LinkConfig(entry));
      }
    } else if (obj instanceof ConfigurationProperties) {
      linkConfigs.add(new LinkConfig((ConfigurationProperties) obj));
    }
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    EndPointConfigFactory.pack(config, this);
    config.put("transformation", linkTransformation);
    config.put("plugin", pluginConnection);
    config.put("cost", cost);
    config.put("groupName", groupName);
    List<ConfigurationProperties> linkProperties = new ArrayList<>();
    for (LinkConfigDTO linkConfig : linkConfigs) {
      linkProperties.add(((Config)linkConfig).toConfigurationProperties());
    }
    config.put("links", linkProperties);
    return config;
  }

  @Override
  public boolean update(BaseConfigDTO update) {
    boolean hasChanged = false;

    if(update instanceof EndPointServerConfigDTO) {
      hasChanged = EndPointConfigFactory.update(this, (EndPointServerConfigDTO) update);

      if (update instanceof EndPointConnectionServerConfigDTO config) {
        if ((this.linkTransformation == null && config.getLinkTransformation() != null)
            || (this.linkTransformation != null
                && !this.linkTransformation.equals(config.getLinkTransformation()))) {
          this.linkTransformation = config.getLinkTransformation();
          hasChanged = true;
        }

        if (!this.authConfig.update(config.getAuthConfig())) {
          hasChanged = true;
        }

        if(cost != config.getCost()) {
          cost = config.getCost();
          hasChanged = true;
        }
        if(!groupName.equals(config.getGroupName())) {
          groupName = config.getGroupName();
          hasChanged = true;
        }

        if (this.pluginConnection != config.isPluginConnection()) {
          pluginConnection = config.isPluginConnection();
          hasChanged = true;
        }

        if (this.linkConfigs.size() != config.getLinkConfigs().size()) {
          this.linkConfigs = config.getLinkConfigs();
          hasChanged = true;
        } else {
          for (int i = 0; i < this.linkConfigs.size(); i++) {
            if (!this.linkConfigs.get(i).equals(config.getLinkConfigs().get(i))) {
              this.linkConfigs.set(i, config.getLinkConfigs().get(i));
              hasChanged = true;
            }
          }
        }
      }
    }
    return hasChanged;
  }
}
