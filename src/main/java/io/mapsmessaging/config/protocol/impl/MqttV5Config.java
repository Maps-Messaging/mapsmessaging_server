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

import io.mapsmessaging.configuration.ConfigurationProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public class MqttV5Config extends MqttConfig {

  private int minServerKeepAlive;
  private int maxServerKeepAlive;

  public MqttV5Config(ConfigurationProperties config) {
    super(config);
    this.minServerKeepAlive = config.getIntProperty("minServerKeepAlive", 0);
    this.maxServerKeepAlive = config.getIntProperty("maxServerKeepAlive", 60);
    setType("mqtt-v5");
  }

  public boolean update(MqttV5Config newConfig) {
    boolean hasChanged = super.update(newConfig);
    if (minServerKeepAlive != newConfig.minServerKeepAlive) {
      minServerKeepAlive = newConfig.minServerKeepAlive;
      hasChanged = true;
    }
    if (maxServerKeepAlive != newConfig.maxServerKeepAlive) {
      maxServerKeepAlive = newConfig.maxServerKeepAlive;
      hasChanged = true;
    }
    return hasChanged;
  }


  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = super.toConfigurationProperties();
    config.put("minServerKeepAlive", minServerKeepAlive);
    config.put("maxServerKeepAlive", maxServerKeepAlive);
    return config;
  }
}
