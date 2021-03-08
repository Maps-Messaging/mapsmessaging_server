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

package org.maps.utilities.configuration;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

public abstract class PropertyManager {

  protected final ConfigurationProperties properties;

  protected PropertyManager(){
    properties = new ConfigurationProperties();
  }

  protected abstract void load();

  protected abstract void store(String name) throws IOException;

  public abstract void copy(PropertyManager propertyManager);

  public @NonNull @NotNull JSONObject getPropertiesJSON(@NonNull @NotNull String name) {
    Object config = properties.get(name);
    if(config instanceof Map){
      Map<String, Object> root = (Map<String, Object>) config;
      Object jsonValue = root.get("JSON");
      if(jsonValue instanceof JSONObject){
        return (JSONObject) jsonValue;
      }
      else if(jsonValue instanceof String){
        return new JSONObject(jsonValue);
      }
    }
    return new JSONObject();
  }

  public void loadPropertiesJSON(@NonNull @NotNull String name, @NonNull @NotNull JSONObject config) {
    properties.remove(name);
    JSONArray array = config.getJSONArray(name);
    Map<String, Object> globalProperties = new LinkedHashMap<>();
    if(config.has("global")) {
      globalProperties = fromJSON(config.getJSONObject("global"));
    }
    for(int x=0;x<array.length();x++){
      Map<String, Object> configEntries = fromJSON(array.getJSONObject(x));
      properties.putAll(configEntries);
    }
    properties.setGlobal(new ConfigurationProperties(globalProperties));
  }

  private Map<String, Object> fromJSON(JSONObject object){
    Map<String, Object> response = new LinkedHashMap<>();
    for(String key:object.keySet()){
      response.put(key, object.getString(key));
    }
    return response;
  }

  public @NonNull @NotNull ConfigurationProperties getProperties(String name) {
    Object obj = properties.get(name);
    if(obj instanceof ConfigurationProperties){
      return (ConfigurationProperties)obj;
    }
    return new ConfigurationProperties();
  }

}
