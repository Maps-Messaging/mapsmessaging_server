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

package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.config.protocol.ProtocolConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public class AmqpConfig extends ProtocolConfig {

  private int idleTimeout = 30000; // Default 30 seconds
  private int maxFrameSize = 65536; // Default 64KB
  private int linkCredit = 50; // Default link credit
  private boolean durable = false; // Link durability
  private int incomingCapacity = 65536; // Session incoming capacity
  private int outgoingWindow = 100; // Session outgoing window

  public AmqpConfig(ConfigurationProperties config) {
    super(config);
    setType("amqp");

    // Initialize fields from config
    this.idleTimeout = config.getIntProperty("idleTimeout", idleTimeout);
    this.maxFrameSize = config.getIntProperty("maxFrameSize", maxFrameSize);
    this.linkCredit = config.getIntProperty("linkCredit", linkCredit);
    this.durable = config.getBooleanProperty("durable", durable);
    this.incomingCapacity = config.getIntProperty("incomingCapacity", incomingCapacity);
    this.outgoingWindow = config.getIntProperty("outgoingWindow", outgoingWindow);
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = super.toConfigurationProperties();
    properties.put("idleTimeout", this.idleTimeout);
    properties.put("maxFrameSize", this.maxFrameSize);
    properties.put("linkCredit", this.linkCredit);
    properties.put("durable", this.durable);
    properties.put("incomingCapacity", this.incomingCapacity);
    properties.put("outgoingWindow", this.outgoingWindow);
    return properties;
  }
}
