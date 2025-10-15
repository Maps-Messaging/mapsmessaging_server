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

package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MqttV5ConfigDTO;

public class MqttV5Config extends MqttV5ConfigDTO implements Config {

  public MqttV5Config(ConfigurationProperties config) {
    setType(config.getProperty("protocol"));
    ProtocolConfigFactory.unpack(config, this);

    // Initialize MQTT V5-specific fields from config
    this.minServerKeepAlive = config.getIntProperty("minServerKeepAlive", minServerKeepAlive);
    this.maxServerKeepAlive = config.getIntProperty("maxServerKeepAlive", maxServerKeepAlive);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof MqttV5ConfigDTO) {
      MqttV5ConfigDTO newConfig = (MqttV5ConfigDTO) config;

      // Check each field and update if necessary
      if (this.minServerKeepAlive != newConfig.getMinServerKeepAlive()) {
        this.minServerKeepAlive = newConfig.getMinServerKeepAlive();
        hasChanged = true;
      }
      if (this.maxServerKeepAlive != newConfig.getMaxServerKeepAlive()) {
        this.maxServerKeepAlive = newConfig.getMaxServerKeepAlive();
        hasChanged = true;
      }

      // Update fields from ProtocolConfigFactory if needed
      if (ProtocolConfigFactory.update(this, newConfig)) {
        hasChanged = true;
      }
    }
    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    ProtocolConfigFactory.pack(properties, this);
    properties.put("minServerKeepAlive", this.minServerKeepAlive);
    properties.put("maxServerKeepAlive", this.maxServerKeepAlive);
    return properties;
  }
}
