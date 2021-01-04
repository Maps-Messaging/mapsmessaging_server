package org.maps.utilities.configuration;


import com.orbitz.consul.ConsulException;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.model.kv.Value;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.json.JSONException;
import org.json.JSONObject;

public class ConsulPropertyManager extends PropertyManager {

  private final String serverPrefix;
  private final KeyValueClient keyValueClient;

  public ConsulPropertyManager(String prefix, ConsulManager manager){
    serverPrefix = prefix+"_";
    keyValueClient = manager.getKeyValueManager();
  }

  protected void load() {
    try {
      List<String> keys = keyValueClient.getKeys(serverPrefix);
      for (String key : keys) {
        try {
          Optional<Value> entry = keyValueClient.getValue(key);
          if (entry.isPresent()) {
            String value = entry.get().getValue().get();
            value = new String(Base64.getDecoder().decode(value));
            System.err.println(value);
            loadPropertiesJSON(key.substring(serverPrefix.length()), new JSONObject(value));
          }
        }
        catch(ConsulException | JSONException consulException){
          consulException.printStackTrace();
        }
      }
    } catch (ConsulException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void store(String name) {
    keyValueClient.putValue(serverPrefix+name, getPropertiesJSON(name).toString(2));
  }

  @Override
  public void copy(PropertyManager propertyManager) {
    // Remove what we have
    for(String name:properties.keySet()){
      keyValueClient.deleteKey(serverPrefix+name);
    }

    // Now lets add the new config
    properties.clear();
    properties.putAll(propertyManager.properties);

    save();
  }

  public void save(){
    // Now lets Store it in key value pairs in consul
    for(String name:properties.keySet()){
      keyValueClient.putValue(serverPrefix+name, getPropertiesJSON(name).toString(2));
    }
  }
}
