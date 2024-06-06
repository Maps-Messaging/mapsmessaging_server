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

package io.mapsmessaging.config;

import io.mapsmessaging.configuration.ConfigurationProperties;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public abstract class Config implements Serializable {

  public abstract ConfigurationProperties toConfigurationProperties();

  protected long parseBufferSize(String size) {
    size = size.trim().toUpperCase();
    if (size.endsWith("K")) {
      return Long.parseLong(size.substring(0, size.length() - 1)) * 1024;
    } else if (size.endsWith("M")) {
      return Long.parseLong(size.substring(0, size.length() - 1)) * 1024 * 1024;
    } else if (size.endsWith("G")) {
      return Long.parseLong(size.substring(0, size.length() - 1)) * 1024 * 1024 * 1024;
    } else {
      return Long.parseLong(size);
    }
  }

  protected String formatBufferSize(long size) {
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

  protected boolean updateMap(Map<String, Object> currentMap, Map<String, Object> newMap) {
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
}
