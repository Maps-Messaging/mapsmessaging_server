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
import io.mapsmessaging.dto.rest.config.protocol.impl.OrbCommDTO;

public class OrbCommConfig extends OrbCommDTO implements Config {

  public OrbCommConfig(ConfigurationProperties config) {
    setType("stogi");
    ProtocolConfigFactory.unpack(config, this);
    serial = new SerialConfig(config);
    initialSetup = config.getProperty("initialSetup", "");
    messagePollInterval = config.getLongProperty("messagePollInterval", 5000);
    ignoreFirstByte = config.getBooleanProperty("ignoreFirstByte", false);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean result = false;
    if (config instanceof OrbCommDTO){
      result = ProtocolConfigFactory.update(this, (OrbCommDTO) config);
      result = ((SerialConfig)serial).update(config) || result;
      if(!initialSetup.equalsIgnoreCase(((OrbCommDTO) config).getInitialSetup())){
        initialSetup = ((OrbCommDTO) config).getInitialSetup();
        result = true;
      }
      if(messagePollInterval != ((OrbCommDTO) config).getMessagePollInterval()){
        messagePollInterval = ((OrbCommDTO) config).getMessagePollInterval();
        result = true;
      }
      if(ignoreFirstByte != ((OrbCommDTO) config).isIgnoreFirstByte()){
        ignoreFirstByte = ((OrbCommDTO) config).isIgnoreFirstByte();
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
    if(serial instanceof SerialConfig){
      properties.put("serial", ((SerialConfig) serial).toConfigurationProperties());
    }
    properties.put("messagePollInterval", messagePollInterval);
    properties.put("ignoreFirstByte", ignoreFirstByte);
    return properties;
  }
}

