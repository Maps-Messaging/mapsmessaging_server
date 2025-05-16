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
import io.mapsmessaging.dto.rest.config.protocol.impl.AmqpConfigDTO;

public class AmqpConfig extends AmqpConfigDTO implements Config {

  public AmqpConfig(ConfigurationProperties config) {
    setType("amqp");
    ProtocolConfigFactory.unpack(config, this);

    // Initialize additional AMQP-specific fields from config
    this.idleTimeout = config.getIntProperty("idleTimeout", idleTimeout);
    this.maxFrameSize = config.getIntProperty("maxFrameSize", maxFrameSize);
    this.linkCredit = config.getIntProperty("linkCredit", linkCredit);
    this.durable = config.getBooleanProperty("durable", durable);
    this.incomingCapacity = config.getIntProperty("incomingCapacity", incomingCapacity);
    this.outgoingWindow = config.getIntProperty("outgoingWindow", outgoingWindow);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof AmqpConfigDTO) {
      AmqpConfigDTO newConfig = (AmqpConfigDTO) config;

      // Check each field and update if necessary
      if (this.idleTimeout != newConfig.getIdleTimeout()) {
        this.idleTimeout = newConfig.getIdleTimeout();
        hasChanged = true;
      }
      if (this.maxFrameSize != newConfig.getMaxFrameSize()) {
        this.maxFrameSize = newConfig.getMaxFrameSize();
        hasChanged = true;
      }
      if (this.linkCredit != newConfig.getLinkCredit()) {
        this.linkCredit = newConfig.getLinkCredit();
        hasChanged = true;
      }
      if (this.durable != newConfig.isDurable()) {
        this.durable = newConfig.isDurable();
        hasChanged = true;
      }
      if (this.incomingCapacity != newConfig.getIncomingCapacity()) {
        this.incomingCapacity = newConfig.getIncomingCapacity();
        hasChanged = true;
      }
      if (this.outgoingWindow != newConfig.getOutgoingWindow()) {
        this.outgoingWindow = newConfig.getOutgoingWindow();
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
    properties.put("idleTimeout", this.idleTimeout);
    properties.put("maxFrameSize", this.maxFrameSize);
    properties.put("linkCredit", this.linkCredit);
    properties.put("durable", this.durable);
    properties.put("incomingCapacity", this.incomingCapacity);
    properties.put("outgoingWindow", this.outgoingWindow);
    return properties;
  }
}
