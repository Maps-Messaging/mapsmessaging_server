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

package io.mapsmessaging.config.device;

import io.mapsmessaging.config.network.SerialDeviceHelper;
import io.mapsmessaging.config.network.impl.SerialConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.device.SerialBusDeviceDTO;

import java.util.Map;

public class SerialDeviceConfig extends SerialBusDeviceDTO implements DeviceBusConfig {

  public SerialDeviceConfig(ConfigurationProperties props) {
    this.name = props.getProperty("name");
    this.selector = props.getProperty("selector", "");
    serialConfig = SerialDeviceHelper.getSerialDeviceDTO(props);
  }


  @Override
  public ConfigurationProperties toConfigurationProperties() {
    return null;
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    return false;
  }

  @Override
  public boolean updateMap(Map<String, Object> currentMap, Map<String, Object> newMap) {
    return DeviceBusConfig.super.updateMap(currentMap, newMap);
  }
}
