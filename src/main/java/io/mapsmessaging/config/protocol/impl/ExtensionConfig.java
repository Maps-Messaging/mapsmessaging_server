/*
 *
 *  Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *  Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.ExtensionConfigDTO;

import java.util.LinkedHashMap;

public class ExtensionConfig extends ExtensionConfigDTO implements Config {

  public ExtensionConfig(ConfigurationProperties configuration) {
    setType("extension");
    ProtocolConfigFactory.unpack(configuration, this);
    Object obj =  configuration.get("config");
    if(obj instanceof ConfigurationProperties){
      config = new LinkedHashMap<>();
      config.putAll(((ConfigurationProperties)obj).getMap());
    }
  }


  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof ExtensionConfig) {
      ExtensionConfig newConfig = (ExtensionConfig) config;
      if (ProtocolConfigFactory.update(this, newConfig)) {
        hasChanged = true;
      }
    }
    return hasChanged;
  }


  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    ProtocolConfigFactory.pack(config, this);
    return config;
  }

  public ExtensionConfig(){}
}
