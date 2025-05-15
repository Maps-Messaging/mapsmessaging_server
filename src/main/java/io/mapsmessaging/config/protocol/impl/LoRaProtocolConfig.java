/*
 *
 *  Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *  Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.LoRaProtocolConfigDTO;

public class LoRaProtocolConfig extends LoRaProtocolConfigDTO implements Config {

  public LoRaProtocolConfig(ConfigurationProperties config) {
    setType("lora");
    ProtocolConfigFactory.unpack(config, this);

    // Initialize LoRa-specific fields from config
    this.retransmit = config.getIntProperty("LoRaMaxTransmissionRate", retransmit);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof LoRaProtocolConfigDTO) {
      LoRaProtocolConfigDTO newConfig = (LoRaProtocolConfigDTO) config;

      // Check each field and update if necessary
      if (this.retransmit != newConfig.getRetransmit()) {
        this.retransmit = newConfig.getRetransmit();
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
    properties.put("LoRaMaxTransmissionRate", this.retransmit);
    return properties;
  }
}
