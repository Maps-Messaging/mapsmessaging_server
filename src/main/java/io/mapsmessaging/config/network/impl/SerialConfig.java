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

package io.mapsmessaging.config.network.impl;

import io.mapsmessaging.config.network.EndPointConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString
public class SerialConfig extends EndPointConfig {

  private String port;
  private int baudRate;
  private int dataBits;
  private String stopBits;
  private String parity;
  private int flowControl;

  private int readTimeOut;
  private int writeTimeOut;
  private int bufferSize;

  public SerialConfig(ConfigurationProperties config) {
    super(config);
    port =  config.getProperty("port");
    baudRate = config.getIntProperty("baudRate", 9600);
    dataBits = config.getIntProperty("dataBits", 8);
    stopBits = config.getProperty("stopBits", "1");
    parity = config.getProperty("parity", "n");
    flowControl = config.getIntProperty("flowControl", 1);

    readTimeOut = config.getIntProperty("readTimeOut", 60000);
    writeTimeOut = config.getIntProperty("writeTimeOut", 60000);
    bufferSize = config.getIntProperty("bufferSize",256 * 1024);
    setType("serial");
  }

  public boolean update(SerialConfig newConfig) {
    boolean hasChanged = super.update(newConfig);
    if(newConfig.getPort() != port) {
      port = newConfig.getPort();
      hasChanged = true;
    }
    if(newConfig.getReadTimeOut() != readTimeOut){
      readTimeOut = newConfig.getReadTimeOut();
      hasChanged = true;
    }
    if(newConfig.getWriteTimeOut() != writeTimeOut){
      writeTimeOut = newConfig.getWriteTimeOut();
      hasChanged = true;
    }
    if(newConfig.getBufferSize() != bufferSize){
      bufferSize = newConfig.getBufferSize();
      hasChanged = true;
    }
    if(newConfig.getFlowControl() != flowControl){
      flowControl = newConfig.getFlowControl();
      hasChanged = true;
    }
    if(newConfig.getParity() != parity){
      parity = newConfig.getParity();
      hasChanged = true;
    }
    if(newConfig.getStopBits() != stopBits){
      stopBits = newConfig.getStopBits();
      hasChanged = true;
    }
    if(newConfig.getBaudRate() != baudRate){
      baudRate = newConfig.getBaudRate();
      hasChanged = true;
    }
    if(newConfig.getDataBits() != dataBits){
      dataBits = newConfig.getDataBits();
      hasChanged = true;
    }
    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = super.toConfigurationProperties();
    config.put("port", port);
    config.put("readTimeOut", readTimeOut);
    config.put("writeTimeOut", writeTimeOut);
    config.put("bufferSize", bufferSize);
    config.put("baudRate", baudRate);
    config.put("dataBits", dataBits);
    config.put("parity", parity);
    config.put("flowControl", flowControl);
    config.put("stopBits", stopBits);
    return config;
  }

}
