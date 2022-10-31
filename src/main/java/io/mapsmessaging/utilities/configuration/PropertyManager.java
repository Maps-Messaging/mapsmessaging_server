/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public abstract class PropertyManager {

  protected final ConfigurationProperties properties;

  protected PropertyManager() {
    properties = new ConfigurationProperties();
  }

  protected abstract void load();

  protected abstract void store(String name) throws IOException;

  public abstract void copy(PropertyManager propertyManager);

  public @NonNull @NotNull JSONObject getPropertiesJSON(@NonNull @NotNull String name) {
    JSONObject jsonObject = new JSONObject();
    Object config = properties.get(name);
    if (config instanceof ConfigurationProperties) {
      config = ((ConfigurationProperties) config).getMap();
    }
    if (config instanceof Map) {
      Map<String, Object> map = pack((Map<String, Object>) config);
      jsonObject.put(name, map);
      if (properties.getGlobal() != null) {
        map.put("global", pack(properties.getGlobal().getMap()));
      }
    } else {
      jsonObject.put(name, config);
    }
    return jsonObject;
  }

  private void pack(Map<String, Object> map, String key, Object obj) {
    if (obj instanceof ConfigurationProperties) {
      pack(map, key, ((ConfigurationProperties) obj).getMap());
    } else if (obj instanceof Map) {
      map.put(key, pack((Map<String, Object>) obj));
    } else if (obj instanceof List) {
      List<Object> list = (List<Object>) obj;
      List<Object> translated = new ArrayList<>();
      for (Object tmp : list) {
        if (tmp instanceof ConfigurationProperties) {
          translated.add(pack(((ConfigurationProperties) tmp).getMap()));
        } else {
          translated.add(tmp);
        }
      }
      map.put(key, translated);
    } else {
      map.put(key, obj);
    }
  }

  private Map<String, Object> pack(Map<String, Object> map) {
    Map<String, Object> tmp = new LinkedHashMap<>();
    for (Entry<String, Object> entry : map.entrySet()) {
      String key = entry.getKey();
      Object obj = entry.getValue();
      pack(tmp, key, obj);
    }
    return tmp;
  }

  public boolean contains(String name){
    return properties.containsKey(name);
  }
  public @NonNull @NotNull ConfigurationProperties getProperties(String name) {
    Object obj = properties.get(name);
    if (obj instanceof ConfigurationProperties) {
      return (ConfigurationProperties) obj;
    }
    return new ConfigurationProperties();
  }

}
