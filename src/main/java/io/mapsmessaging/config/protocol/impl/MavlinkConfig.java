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
import io.mapsmessaging.config.protocol.PredefinedTopics;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MqttSnConfigDTO;

import java.util.ArrayList;
import java.util.List;

public class MavlinkConfig extends MavlinkConfigDTO implements Config {

  public MavlinkConfig(ConfigurationProperties config) {
    setType("mavlink");
    ProtocolConfigFactory.unpack(config, this);

    this.idleSessionTimeout = config.getLongProperty("idleSessionTimeout", idleSessionTimeout);
    this.maximumSessionExpiry = config.getIntProperty("maximumSessionExpiry", maximumSessionExpiry);
    this.advertiseInterval = config.getIntProperty("advertiseInterval", advertiseInterval);
    this.maxInFlightEvents = config.getIntProperty("maxInFlightEvents", maxInFlightEvents);
    this.topicNameTemplate = config.getProperty("topicNameTemplate", topicNameTemplate);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof MavlinkConfigDTO newConfig) {
      if(!topicNameTemplate.equals(newConfig.getTopicNameTemplate())) {
        this.topicNameTemplate = newConfig.getTopicNameTemplate();
        hasChanged = true;
      }
      if (this.idleSessionTimeout != newConfig.getIdleSessionTimeout()) {
        this.idleSessionTimeout = newConfig.getIdleSessionTimeout();
        hasChanged = true;
      }
      if (this.maximumSessionExpiry != newConfig.getMaximumSessionExpiry()) {
        this.maximumSessionExpiry = newConfig.getMaximumSessionExpiry();
        hasChanged = true;
      }
      if (this.advertiseInterval != newConfig.getAdvertiseInterval()) {
        this.advertiseInterval = newConfig.getAdvertiseInterval();
        hasChanged = true;
      }
      if (this.maxInFlightEvents != newConfig.getMaxInFlightEvents()) {
        this.maxInFlightEvents = newConfig.getMaxInFlightEvents();
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
    properties.put("idleSessionTimeout", this.idleSessionTimeout);
    properties.put("maximumSessionExpiry", this.maximumSessionExpiry);
    properties.put("advertiseInterval", this.advertiseInterval);
    properties.put("maxInFlightEvents", this.maxInFlightEvents);
    properties.put("topicNameTemplate", this.topicNameTemplate);
    return properties;
  }
}
