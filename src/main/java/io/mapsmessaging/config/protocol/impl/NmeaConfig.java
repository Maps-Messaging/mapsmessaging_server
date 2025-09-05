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
import io.mapsmessaging.config.network.impl.SerialConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.NmeaConfigDTO;

public class NmeaConfig extends NmeaConfigDTO implements Config {

  public NmeaConfig(ConfigurationProperties config) {
    setType("NMEA-0183");
    ProtocolConfigFactory.unpack(config, this);
    serial = new SerialConfig(config);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean result = false;
    if (config instanceof NmeaConfigDTO){
      result = ProtocolConfigFactory.update(this, (NmeaConfigDTO) config);
      result = ((SerialConfig)serial).update(config) || result;
    }
    return result;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    ProtocolConfigFactory.pack(properties, this);
    if(serial instanceof SerialConfig){
      properties.put("serial", ((SerialConfig) serial).toConfigurationProperties());
    }
    return properties;
  }
}
