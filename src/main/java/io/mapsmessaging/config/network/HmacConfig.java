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

package io.mapsmessaging.config.network;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.HmacConfigDTO;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString
public class HmacConfig extends HmacConfigDTO implements Config {

  public HmacConfig(ConfigurationProperties config) {
    super();
    this.host = config.getProperty("host");
    this.port = config.getIntProperty("port", 0);
    this.hmacAlgorithm = config.getProperty("HmacAlgorithm");
    if (hmacAlgorithm != null) {
      this.hmacManager = config.getProperty("HmacManager", "Appender");
      this.hmacSharedKey = config.getProperty("HmacSharedKey");
    }
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("host", host);
    config.put("port", port);
    config.put("HmacAlgorithm", hmacAlgorithm);
    config.put("HmacManager", hmacManager);
    config.put("HmacSharedKey", hmacSharedKey);
    return config;
  }

  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;

    if( !(config instanceof HmacConfigDTO) ) {
      return false;
    }
    HmacConfigDTO newConfig = (HmacConfigDTO)config;

    if (this.host == null || !this.host.equals(newConfig.getHost())) {
      this.host = newConfig.getHost();
      hasChanged = true;
    }
    if (this.port != newConfig.getPort()) {
      this.port = newConfig.getPort();
      hasChanged = true;
    }
    if (this.hmacAlgorithm == null || !this.hmacAlgorithm.equals(newConfig.getHmacAlgorithm())) {
      this.hmacAlgorithm = newConfig.getHmacAlgorithm();
      hasChanged = true;
    }
    if (this.hmacManager == null || !this.hmacManager.equals(newConfig.getHmacManager())) {
      this.hmacManager = newConfig.getHmacManager();
      hasChanged = true;
    }
    if (this.hmacSharedKey == null || !this.hmacSharedKey.equals(newConfig.getHmacSharedKey())) {
      this.hmacSharedKey = newConfig.getHmacSharedKey();
      hasChanged = true;
    }
    return hasChanged;
  }
}
