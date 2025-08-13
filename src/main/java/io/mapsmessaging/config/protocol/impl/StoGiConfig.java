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
import io.mapsmessaging.dto.rest.config.protocol.impl.StoGiConfigDTO;

public class StoGiConfig extends StoGiConfigDTO implements Config {

  public StoGiConfig(ConfigurationProperties config) {
    setType("stogi");
    ProtocolConfigFactory.unpack(config, this);
    serial = new SerialConfig(config);
    initialSetup = config.getProperty("initialSetup", "");
    messagePollInterval = config.getLongProperty("messagePollInterval", 5000);
    ignoreFirstByte = config.getBooleanProperty("ignoreFirstByte", false);
    modemResponseTimeout = config.getLongProperty("modemResponseTimeout", 5000);
    setServerLocation = config.getBooleanProperty("setServerLocation", true);
    modemStatsTopic = config.getProperty("modemStatsTopic", "");
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean result = false;
    if (config instanceof StoGiConfigDTO orbCommConfig) {
      result = ProtocolConfigFactory.update(this, orbCommConfig);
      result = ((SerialConfig)serial).update(config) || result;
      if(!modemStatsTopic.equals(orbCommConfig.getModemStatsTopic())){
        modemStatsTopic = orbCommConfig.getModemStatsTopic();
        result = true;
      }
      if(!initialSetup.equalsIgnoreCase(orbCommConfig.getInitialSetup())){
        initialSetup = orbCommConfig.getInitialSetup();
        result = true;
      }
      if(messagePollInterval != orbCommConfig.getMessagePollInterval()){
        messagePollInterval = orbCommConfig.getMessagePollInterval();
        result = true;
      }
      if(ignoreFirstByte != orbCommConfig.isIgnoreFirstByte()){
        ignoreFirstByte = orbCommConfig.isIgnoreFirstByte();
        result = true;
      }
      if (modemResponseTimeout != orbCommConfig.getModemResponseTimeout()) {
        modemResponseTimeout = orbCommConfig.getModemResponseTimeout();
        result = true;
      }
      if(setServerLocation != orbCommConfig.isSetServerLocation()){
        setServerLocation = orbCommConfig.isSetServerLocation();
        result = true;
      }
    }
    return result;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    ProtocolConfigFactory.pack(properties, this);
    properties.put("initialSetup", initialSetup);
    if(serial instanceof SerialConfig serialConfig){
      properties.put("serial", serialConfig.toConfigurationProperties());
    }
    properties.put("messagePollInterval", messagePollInterval);
    properties.put("ignoreFirstByte", ignoreFirstByte);
    properties.put("modemResponseTimeout", modemResponseTimeout);
    properties.put("setServerLocation", setServerLocation);
    return properties;
  }
}

