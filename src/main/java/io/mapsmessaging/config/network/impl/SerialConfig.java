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

package io.mapsmessaging.config.network.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.config.network.SerialDeviceHelper;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.SerialDeviceDTO;
import io.mapsmessaging.dto.rest.config.network.impl.SerialConfigDTO;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString
public class SerialConfig extends SerialConfigDTO implements Config {


  public SerialConfig(ConfigurationProperties config) {
    NetworkConfigFactory.unpack(config, this);
    if (config.containsKey("serial")) {
      ConfigurationProperties serial = (ConfigurationProperties) config.get("serial");
      serialDevice = SerialDeviceHelper.getSerialDeviceDTO(serial);
    }
    else{
      serialDevice = SerialDeviceHelper.getSerialDeviceDTO(config);
    }
    setType("serial");
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if(config instanceof SerialConfigDTO){
      SerialConfigDTO newConfig = (SerialConfigDTO) config;

    }
    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    NetworkConfigFactory.pack(config, this);

    return config;
  }
}
