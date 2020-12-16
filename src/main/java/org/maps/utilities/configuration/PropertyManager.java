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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.utilities.ResourceList;

public class PropertyManager {

  private static final PropertyManager instance;

  static {
    instance = new PropertyManager();
  }

  private final Logger logger = LoggerFactory.getLogger(PropertyManager.class);
  private final HashMap<String, HashMap<Integer, ConfigurationProperties>> properties;

  private PropertyManager() {
    logger.log(LogMessages.PROPERTY_MANAGER_START);
    properties = new LinkedHashMap<>();

    try {
      Collection<String> knownProperties = ResourceList.getResources(Pattern.compile(".*props"));
      for (String propertyName : knownProperties) {
        loadProperty(propertyName);
      }
    } catch (IOException e) {
      logger.log(LogMessages.PROPERTY_MANAGER_SCAN_FAILED, e);
    }
  }

  public static PropertyManager getInstance() {
    return instance;
  }

  private void loadProperty(String propertyName) {
    try {
      propertyName = propertyName.substring(propertyName.lastIndexOf(File.separatorChar) + 1);
      propertyName = propertyName.substring(0, propertyName.indexOf(".props"));
      ConfigurationProperties prop = new ConfigurationProperties(propertyName);
      properties.put(propertyName, scan(prop));
      logger.log(LogMessages.PROPERTY_MANAGER_FOUND, propertyName);
    } catch (IOException e) {
      logger.log(LogMessages.PROPERTY_MANAGER_LOAD_FAILED, e, propertyName);
    }
  }

  private @NotNull HashMap<Integer, ConfigurationProperties> scan(ConfigurationProperties props) {
    logger.log(LogMessages.PROPERTY_MANAGER_SCANNING, props.size());

    HashMap<Integer, ConfigurationProperties> list = new LinkedHashMap<>();

    HashMap<String, String> globals = scanForGlobalList(props);

    removeGlobalFromProperties(globals, props);

    //
    // Ok this property is a nested property and has multiple entries in it
    //
    if (props.isEmpty()) {
      list.put(0, new ConfigurationProperties(globals));
    } else {
      logger.log(LogMessages.PROPERTY_MANAGER_INDEX_DETECTED);
      LinkedHashMap<Integer, HashMap<String, String>> parsed = buildIndexedProperties(props);
      addGlobalKeys(parsed, globals, list);
    }
    return list;
  }

  private @NotNull LinkedHashMap<Integer, HashMap<String, String>> buildIndexedProperties(ConfigurationProperties props) {
    LinkedHashMap<Integer, HashMap<String, String>> parsed = new LinkedHashMap<>();
    for (Map.Entry<Object, Object> entry : props.entrySet()) {
      String key = entry.getKey().toString();
      int index = key.lastIndexOf('_');
      if (index > 0) {
        String end = key.substring(index + 1).trim();
        int idx = Integer.parseInt(end);
        HashMap<String, String> tmpList = parsed.computeIfAbsent(idx, k -> new HashMap<>());
        String tmp = entry.getKey().toString();
        tmp = tmp.substring(0, index);
        tmpList.put(tmp, entry.getValue().toString());
      }
    }
    return parsed;
  }

  private void addGlobalKeys(
      LinkedHashMap<Integer, HashMap<String, String>> parsed,
      HashMap<String, String> globals,
      HashMap<Integer, ConfigurationProperties> list) {
    for (Map.Entry<Integer, HashMap<String, String>> entry : parsed.entrySet()) {
      HashMap<String, String> hashed = entry.getValue();
      //
      // Add global overrides IF the specific interface does not have it defined
      //
      for (Map.Entry<String, String> global : globals.entrySet()) {
        if (!hashed.containsKey(global.getKey())) {
          hashed.put(global.getKey(), global.getValue());
        }
      }
      list.put(entry.getKey(), new ConfigurationProperties(hashed));
      logger.log(LogMessages.PROPERTY_MANAGER_COMPLETED_INDEX, hashed.size(), entry.getKey());
    }
  }

  private boolean isNumber(String key) {
    int index = key.lastIndexOf('_');
    if (index > 0) {
      String end = key.substring(index + 1).trim();
      try {
        Integer.parseInt(end);
      } catch (NumberFormatException e) {
        return false;
      }
    } else {
      return false;
    }
    return true;
  }

  //
  // Create the global list of properties
  //
  private @NotNull HashMap<String, String> scanForGlobalList(ConfigurationProperties props) {
    HashMap<String, String> globals = new LinkedHashMap<>();
    for (Map.Entry<Object, Object> entry : props.entrySet()) {
      String key = entry.getKey().toString();
      if (!isNumber(key)) {
        globals.put(key, entry.getValue().toString());
      }
    }
    return globals;
  }

  //
  // Remove the global from the list
  //
  private void removeGlobalFromProperties(HashMap<String, String> globals, ConfigurationProperties props) {
    for (Map.Entry<String, String> global : globals.entrySet()) {
      props.remove(global.getKey());
    }
  }

  public @Nullable Map<Integer, ConfigurationProperties> getPropertiesList(String name) {
    return properties.get(name);
  }

  public @NotNull ConfigurationProperties getProperties(String name) {
    HashMap<Integer, ConfigurationProperties> entry = properties.get(name);
    if(entry != null) {
      return entry.get(0);
    }
    return new ConfigurationProperties(new HashMap<>());
  }
}
