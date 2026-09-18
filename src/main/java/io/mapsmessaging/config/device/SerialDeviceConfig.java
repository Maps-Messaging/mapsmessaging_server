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
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.device.SerialBusDeviceDTO;
import io.mapsmessaging.dto.rest.config.network.SerialDeviceDTO;

import java.util.Map;
import java.util.Objects;

public class SerialDeviceConfig extends SerialBusDeviceDTO implements DeviceBusConfig {

  public SerialDeviceConfig(ConfigurationProperties props) {
    this.name = props.getProperty("name");
    this.selector = props.getProperty("selector", "");
    serialConfig = SerialDeviceHelper.getSerialDeviceDTO(props);
    readTimeOut = serialConfig.getReadTimeOut();
    writeTimeOut = serialConfig.getWriteTimeOut();
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("name", this.name);
    if (this.selector != null && !this.selector.isEmpty()) {
      props.put("selector", this.selector);
    }

    if (serialConfig != null) {
      ConfigurationProperties serial = new ConfigurationProperties();
      serial.put("port", serialConfig.getPort());
      serial.put("baudRate", serialConfig.getBaudRate());
      serial.put("dataBits", serialConfig.getDataBits());
      serial.put("stopBits", serialConfig.getStopBits());
      serial.put("parity", serialConfig.getParity());
      serial.put("flowControl", serialConfig.getFlowControl());
      if (serialConfig.getSerialNo() != null) {
        serial.put("serialNo", serialConfig.getSerialNo());
      }
      props.put("serial", serial);
      props.put("readTimeOut", readTimeOut);
      props.put("writeTimeOut", writeTimeOut);
      props.put("bufferSize", serialConfig.getBufferSize());
    }
    return props;
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof SerialBusDeviceDTO)) {
      return false;
    }

    SerialBusDeviceDTO newConfig = (SerialBusDeviceDTO) config;
    boolean hasChanged = false;

    if (!Objects.equals(this.name, newConfig.getName())) {
      this.name = newConfig.getName();
      hasChanged = true;
    }
    if (!Objects.equals(this.selector, newConfig.getSelector())) {
      this.selector = newConfig.getSelector();
      hasChanged = true;
    }
    if (!Objects.equals(this.serialConfig, newConfig.getSerialConfig())) {
      this.serialConfig = newConfig.getSerialConfig();
      hasChanged = true;
    }
    if (this.readTimeOut != newConfig.getReadTimeOut()) {
      this.readTimeOut = newConfig.getReadTimeOut();
      hasChanged = true;
    }
    if (this.writeTimeOut != newConfig.getWriteTimeOut()) {
      this.writeTimeOut = newConfig.getWriteTimeOut();
      hasChanged = true;
    }
    if (serialConfig != null) {
      serialConfig.setReadTimeOut(readTimeOut);
      serialConfig.setWriteTimeOut(writeTimeOut);
    }
    return hasChanged;
  }

  @Override
  public boolean updateMap(Map<String, Object> currentMap, Map<String, Object> newMap) {
    return DeviceBusConfig.super.updateMap(currentMap, newMap);
  }
}
