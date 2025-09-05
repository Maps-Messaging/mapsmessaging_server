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

package io.mapsmessaging.config;

import io.mapsmessaging.configuration.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ConfigHelper {


  public static Map<String, Object> buildMap(ConfigurationProperties props){
    Map<String, Object> map = new LinkedHashMap<>();
    if(props != null){
      for(Map.Entry<String, Object> entry: props.entrySet()){
        if(entry.getValue() instanceof ConfigurationProperties){
          map.put(entry.getKey(), buildMap((ConfigurationProperties) entry.getValue()));
        }
        else{
          map.put(entry.getKey(), props.getProperty(entry.getKey())); // Translations maybe done here
        }
      }
    }
    return map;
  }

  public static boolean updateMap(Map<String, Object> currentMap, Map<String, Object> newMap) {
    boolean hasChanged = false;

    for (Map.Entry<String, Object> entry : newMap.entrySet()) {
      String key = entry.getKey();
      Object newValue = entry.getValue();

      if (!Objects.equals(currentMap.get(key), newValue)) {
        currentMap.put(key, newValue);
        hasChanged = true;
      }
    }

    return hasChanged;
  }

  public static long parseBufferSize(String size) {
    size = size.trim().toUpperCase();
    if (size.endsWith("K")) {
      return Long.parseLong(size.substring(0, size.length() - 1)) * 1024;
    } else if (size.endsWith("M")) {
      return Long.parseLong(size.substring(0, size.length() - 1)) * 1024 * 1024;
    } else if (size.endsWith("G")) {
      return Long.parseLong(size.substring(0, size.length() - 1)) * 1024 * 1024 * 1024;
    } else {
      if(size.contains(".")){
        size = size.substring(0, size.indexOf("."));
      }
      return Long.parseLong(size);
    }
  }

  public static String formatBufferSize(long size) {
    if (size >= 1024 * 1024 * 1024) {
      return (size / (1024 * 1024 * 1024)) + "G";
    } else if (size >= 1024 * 1024) {
      return (size / (1024 * 1024)) + "M";
    } else if (size >= 1024) {
      return (size / 1024) + "K";
    } else {
      return Long.toString(size);
    }
  }

  private ConfigHelper(){}
}
