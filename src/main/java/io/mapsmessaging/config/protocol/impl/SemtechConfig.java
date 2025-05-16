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
import io.mapsmessaging.dto.rest.config.protocol.impl.SemtechConfigDTO;

public class SemtechConfig extends SemtechConfigDTO implements Config {

  public SemtechConfig(ConfigurationProperties config) {
    setType("semtech");
    ProtocolConfigFactory.unpack(config, this);

    // Initialize Semtech-specific fields from config
    this.maxQueued = config.getIntProperty("MaxQueueSize", maxQueued);
    this.inboundTopicName = config.getProperty("inbound", inboundTopicName);
    this.outboundTopicName = config.getProperty("outbound", outboundTopicName);
    this.statusTopicName = config.getProperty("status", inboundTopicName);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof SemtechConfigDTO) {
      SemtechConfigDTO newConfig = (SemtechConfigDTO) config;

      // Check each field and update if necessary
      if (this.maxQueued != newConfig.getMaxQueued()) {
        this.maxQueued = newConfig.getMaxQueued();
        hasChanged = true;
      }
      if (!this.inboundTopicName.equals(newConfig.getInboundTopicName())) {
        this.inboundTopicName = newConfig.getInboundTopicName();
        hasChanged = true;
      }
      if (!this.outboundTopicName.equals(newConfig.getOutboundTopicName())) {
        this.outboundTopicName = newConfig.getOutboundTopicName();
        hasChanged = true;
      }
      if (!this.statusTopicName.equals(newConfig.getStatusTopicName())) {
        this.statusTopicName = newConfig.getStatusTopicName();
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
    properties.put("MaxQueueSize", this.maxQueued);
    properties.put("inbound", this.inboundTopicName);
    properties.put("outbound", this.outboundTopicName);
    properties.put("status", this.statusTopicName);
    return properties;
  }
}
