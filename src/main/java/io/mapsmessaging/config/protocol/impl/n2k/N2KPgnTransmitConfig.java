/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.config.protocol.impl.n2k;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.protocol.impl.n2k.N2KPgnTransmitConfigDTO;

public class N2KPgnTransmitConfig extends N2KPgnTransmitConfigDTO {

  public N2KPgnTransmitConfig(ConfigurationProperties config) {
    this.enabled = config.getBooleanProperty("enabled", enabled);
    this.intervalMilliseconds = config.getLongProperty("intervalMilliseconds", intervalMilliseconds);
  }

  public boolean update(N2KPgnTransmitConfig newConfig) {
    boolean hasChanged = false;
    if (enabled != newConfig.isEnabled()) {
      enabled = newConfig.isEnabled();
      hasChanged = true;
    }

    if (intervalMilliseconds != newConfig.getIntervalMilliseconds()) {
      intervalMilliseconds = newConfig.getIntervalMilliseconds();
      hasChanged = true;
    }
    return hasChanged;
  }

  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();

    properties.put("enabled", enabled);
    properties.put("intervalMilliseconds", intervalMilliseconds);

    return properties;
  }
}