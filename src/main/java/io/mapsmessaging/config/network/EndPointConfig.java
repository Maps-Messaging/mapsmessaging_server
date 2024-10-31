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

package io.mapsmessaging.config.network;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public class EndPointConfig extends Config {

  private boolean discoverable;
  private int selectorThreadCount;
  private long serverReadBufferSize;
  private long serverWriteBufferSize;

  public EndPointConfig(ConfigurationProperties config) {
    this.selectorThreadCount = config.getIntProperty("selectorThreadCount", 2);
    this.discoverable = config.getBooleanProperty("discoverable", false);
    this.serverReadBufferSize = parseBufferSize(config.getProperty("serverReadBufferSize", "10K"));
    this.serverWriteBufferSize =
        parseBufferSize(config.getProperty("serverWriteBufferSize", "10K"));
  }

  public boolean update(EndPointConfig newConfig) {
    boolean hasChanged = false;
    if (this.selectorThreadCount != newConfig.getSelectorThreadCount()) {
      this.selectorThreadCount = newConfig.getSelectorThreadCount();
      hasChanged = true;
    }
    if (this.discoverable != newConfig.isDiscoverable()) {
      this.discoverable = newConfig.isDiscoverable();
      hasChanged = true;
    }
    if (this.serverReadBufferSize != newConfig.getServerReadBufferSize()) {
      this.serverReadBufferSize = newConfig.getServerReadBufferSize();
      hasChanged = true;
    }
    if (this.serverWriteBufferSize != newConfig.getServerWriteBufferSize()) {
      this.serverWriteBufferSize = newConfig.getServerWriteBufferSize();
      hasChanged = true;
    }
    return hasChanged;
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("selectorThreadCount", this.selectorThreadCount);
    config.put("discoverable", this.discoverable);
    config.put("serverReadBufferSize", formatBufferSize(this.serverReadBufferSize));
    config.put("serverWriteBufferSize", formatBufferSize(this.serverWriteBufferSize));
    return config;
  }
}
