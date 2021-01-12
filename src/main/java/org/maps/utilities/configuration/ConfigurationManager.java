/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.utilities.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.messaging.consul.ConsulManagerFactory;

public class ConfigurationManager {

  private static final ConfigurationManager instance;

  static {
    instance = new ConfigurationManager();
  }

  private final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

  private final List<PropertyManager> propertyManagers;
  private PropertyManager authoritive;

  private ConfigurationManager() {
    logger.log(LogMessages.PROPERTY_MANAGER_START);
    propertyManagers = new ArrayList<>();
    authoritive = null;
  }

  public static ConfigurationManager getInstance() {
    return instance;
  }

  public void initialise(@NotNull String serverId){
    if(ConsulManagerFactory.getInstance().isStarted()){
      authoritive = new ConsulPropertyManager(serverId);
      authoritive.load();
      propertyManagers.add(new ConsulPropertyManager("default_"));
    }
    propertyManagers.add(new FilePropertyManager());
    for(PropertyManager manager:propertyManagers){
      manager.load();
    }
  }

  //
  // Build up a list of property names from all the property managers
  //
  public Set<String> getPropertyNames(){
    Set<String> response = new HashSet<>();
    for(PropertyManager manager:propertyManagers){
      Set<String> names = manager.getPropertyNames();
      if(names != null) {
        for (String name : names) {
          if (!response.contains(name)) {
            response.add(name);
          }
        }
      }
    }
    return response;
  }

  public @Nullable Map<Integer, ConfigurationProperties> getPropertiesList(String name) {
    Map<Integer, ConfigurationProperties> map = null;
    if(authoritive != null){
      map = authoritive.getPropertiesList(name);
      if(map != null){
        return map;
      }
    }
    for(PropertyManager manager:propertyManagers) {
      map = manager.getPropertiesList(name);
      if(map != null){
        if(authoritive != null) {
          authoritive.properties.put(name, map);
          authoritive.store(name);
        }
        return map;
      }
    }
    return null;
  }

  public @NotNull ConfigurationProperties getProperties(String name) {
    ConfigurationProperties config;
    if(authoritive != null){
      config = authoritive.getProperties(name);
      if(!config.isEmpty()){
        return config;
      }
    }
    for(PropertyManager manager:propertyManagers) {
      ConfigurationProperties entry = manager.getProperties(name);
      if(!entry.isEmpty()){
        Map<Integer, ConfigurationProperties> map = new LinkedHashMap<>();
        map.put(0, entry);
        if(authoritive != null) {
          authoritive.properties.put(name, map);
          authoritive.store(name);
        }
        return entry;
      }
    }
    return new ConfigurationProperties(new HashMap<>(), null);
  }
}
