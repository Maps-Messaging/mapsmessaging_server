package org.maps.utilities.configuration;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

public abstract class PropertyManager {

  protected final Map<String, Map<Integer, ConfigurationProperties>> properties;

  public PropertyManager(){
    properties = new LinkedHashMap<>();
  }

  protected abstract void load();

  protected abstract void store(String name);

  public abstract void copy(PropertyManager propertyManager);

  public @NotNull JSONObject getPropertiesJSON(@NotNull String name) {
    JSONObject object = new JSONObject();
    Map<Integer, ConfigurationProperties> entry = properties.get(name);
    if (entry != null) {
      JSONArray array = new JSONArray();
      Map<String, String> globalEntries = null;
      for (ConfigurationProperties properties : entry.values()) {
        array.put(properties.toJSON());
        if(globalEntries == null && !properties.getGlobalValues().isEmpty()){
          globalEntries = properties.getGlobalValues();
        }
      }

      // If we have global entries then we simply add them to the object
      if(globalEntries != null){
        object.put("global", toJSON(globalEntries));
      }
      object.put(name, array);
    }
    return object;
  }

  public void loadPropertiesJSON(@NotNull String name, @NotNull JSONObject config) {
    properties.remove(name);
    JSONArray array = config.getJSONArray(name);
    Map<String, String> globalProperties = new LinkedHashMap<>();
    if(config.has("global")) {
      globalProperties = fromJSON(config.getJSONObject("global"));
    }
    Map<Integer, ConfigurationProperties> entries = new LinkedHashMap<>();
    for(int x=0;x<array.length();x++){
      Map<String, String> configEntries = fromJSON(array.getJSONObject(x));
      ConfigurationProperties configurationProperties = new ConfigurationProperties(configEntries, globalProperties);
      entries.put(x, configurationProperties);
    }
    properties.put(name, entries);
  }

  private Map<String, String> fromJSON(JSONObject object){
    Map<String, String> response = new LinkedHashMap<>();
    for(String key:object.keySet()){
      response.put(key, object.getString(key));
    }
    return response;
  }

  private JSONObject toJSON(Map<String, String> map){
    JSONObject response = new JSONObject();
    for(Entry<String, String> entry:map.entrySet()){
      response.put(entry.getKey(), entry.getValue());
    }
    return response;
  }

  public Set<String> getPropertyNames() {
    return properties.keySet();
  }

  public @Nullable Map<Integer, ConfigurationProperties> getPropertiesList(String name) {
    return properties.get(name);
  }

  public @NotNull ConfigurationProperties getProperties(String name) {
    Map<Integer, ConfigurationProperties> entry = properties.get(name);
    if (entry != null) {
      return entry.get(0);
    }
    return new ConfigurationProperties(new HashMap<>(), null);
  }

}
