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

package io.mapsmessaging.config.network.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
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
      this.setPort(serial.getProperty("port"));
      this.setBaudRate(serial.getIntProperty("baudRate", 9600));
      this.setDataBits(serial.getIntProperty("dataBits", 8));
      this.setStopBits(serial.getProperty("stopBits", "1"));
      this.setParity(serial.getProperty("parity", "n"));
      this.setFlowControl(serial.getIntProperty("flowControl", 1));
      this.setSerialNo(serial.getProperty("serialNo"));
    }

    this.setReadTimeOut(config.getIntProperty("readTimeOut", 60000));
    this.setWriteTimeOut(config.getIntProperty("writeTimeOut", 60000));
    this.setBufferSize(config.getIntProperty("bufferSize", 256 * 1024));
    setType("serial");
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if(config instanceof SerialConfigDTO){
      SerialConfigDTO newConfig = (SerialConfigDTO) config;
      hasChanged = NetworkConfigFactory.update(this, newConfig);
      if (!getPort().equals(newConfig.getPort())) {
        setPort(newConfig.getPort());
        hasChanged = true;
      }
      if (getBaudRate() != newConfig.getBaudRate()) {
        setBaudRate(newConfig.getBaudRate());
        hasChanged = true;
      }
      if (getDataBits() != newConfig.getDataBits()) {
        setDataBits(newConfig.getDataBits());
        hasChanged = true;
      }
      if (!getStopBits().equals(newConfig.getStopBits())) {
        setStopBits(newConfig.getStopBits());
        hasChanged = true;
      }
      if (!getParity().equals(newConfig.getParity())) {
        setParity(newConfig.getParity());
        hasChanged = true;
      }
      if (getFlowControl() != newConfig.getFlowControl()) {
        setFlowControl(newConfig.getFlowControl());
        hasChanged = true;
      }
      if (getReadTimeOut() != newConfig.getReadTimeOut()) {
        setReadTimeOut(newConfig.getReadTimeOut());
        hasChanged = true;
      }
      if (getWriteTimeOut() != newConfig.getWriteTimeOut()) {
        setWriteTimeOut(newConfig.getWriteTimeOut());
        hasChanged = true;
      }
      if (getBufferSize() != newConfig.getBufferSize()) {
        setBufferSize(newConfig.getBufferSize());
        hasChanged = true;
      }
      if (!getSerialNo().equals(newConfig.getSerialNo())) {
        setSerialNo(newConfig.getSerialNo());
        hasChanged = true;
      }
    }
    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    NetworkConfigFactory.pack(config, this);
    config.put("port", getPort());
    config.put("baudRate", getBaudRate());
    config.put("dataBits", getDataBits());
    config.put("stopBits", getStopBits());
    config.put("parity", getParity());
    config.put("flowControl", getFlowControl());
    config.put("readTimeOut", getReadTimeOut());
    config.put("writeTimeOut", getWriteTimeOut());
    config.put("bufferSize", getBufferSize());
    config.put("serialNo", getSerialNo());
    return config;
  }
}
