/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.utilities.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import io.mapsmessaging.logging.LogMessages;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.consul.ConsulManagerFactory;

public class ConfigurationManager {

  private static final ConfigurationManager instance;

  static {
    instance = new ConfigurationManager();
  }

  private final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

  private final List<PropertyManager> propertyManagers;
  private PropertyManager authoritative;

  private ConfigurationManager() {
    logger.log(LogMessages.PROPERTY_MANAGER_START);
    propertyManagers = new ArrayList<>();
    authoritative = null;
  }

  public static ConfigurationManager getInstance() {
    return instance;
  }

  public void initialise(@NonNull @NotNull String serverId){
    if(ConsulManagerFactory.getInstance().isStarted()){
      authoritative = new ConsulPropertyManager(serverId);
      authoritative.load();
      propertyManagers.add(new ConsulPropertyManager("default_"));
    }
    propertyManagers.add(new YamlPropertyManager());
    for(PropertyManager manager:propertyManagers){
      manager.load();
    }
  }

  public @NonNull @NotNull ConfigurationProperties getProperties(String name) {
    ConfigurationProperties config;
    if(authoritative != null){
      config = authoritative.getProperties(name);
      if(!config.isEmpty()){
        return config;
      }
    }

    for(PropertyManager manager:propertyManagers) {
      config = manager.getProperties(name);
      if(!config.isEmpty()){
        return config;
      }
    }
    return new ConfigurationProperties(new HashMap<>());
  }
}
