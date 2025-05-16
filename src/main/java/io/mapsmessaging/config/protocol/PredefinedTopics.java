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

package io.mapsmessaging.config.protocol;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.PredefinedTopicsDTO;

public class PredefinedTopics extends PredefinedTopicsDTO implements Config {

  public PredefinedTopics(ConfigurationProperties config) {
    this.id = config.getIntProperty("id", 0);
    this.topic = config.getProperty("topic", "");
    this.address = config.getProperty("address", "*");
  }


  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;

    if (config instanceof PredefinedTopicsDTO) {
    PredefinedTopicsDTO newConfig = (PredefinedTopicsDTO) config;
      if (this.id != newConfig.getId()) {
        this.id = newConfig.getId();
        hasChanged = true;
      }
      if (!this.topic.equals(newConfig.getTopic())) {
        this.topic = newConfig.getTopic();
        hasChanged = true;
      }
      if (!this.address.equals(newConfig.getAddress())) {
        this.address = newConfig.getAddress();
        hasChanged = true;
      }
    }
    return hasChanged;
  }


  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("id", this.id);
    config.put("topic", this.topic);
    config.put("address", this.address);
    return config;
  }
}
