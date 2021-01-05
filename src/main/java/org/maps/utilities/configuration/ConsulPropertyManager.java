package org.maps.utilities.configuration;


import com.orbitz.consul.ConsulException;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.model.kv.Value;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.json.JSONException;
import org.json.JSONObject;
import org.maps.messaging.consul.ConsulManagerFactory;

public class ConsulPropertyManager extends PropertyManager {

  private final String serverPrefix;

  public ConsulPropertyManager(String prefix){
    serverPrefix = prefix+"_";
  }

  protected void load() {
    try {
      KeyValueClient keyValueClient = ConsulManagerFactory.getInstance().getManager().getKeyValueManager();
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
    KeyValueClient keyValueClient = ConsulManagerFactory.getInstance().getManager().getKeyValueManager();
    keyValueClient.putValue(serverPrefix+name, getPropertiesJSON(name).toString(2));
  }

  @Override
  public void copy(PropertyManager propertyManager) {
    KeyValueClient keyValueClient = ConsulManagerFactory.getInstance().getManager().getKeyValueManager();
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
    KeyValueClient keyValueClient = ConsulManagerFactory.getInstance().getManager().getKeyValueManager();
    // Now lets Store it in key value pairs in consul
    for(String name:properties.keySet()){
      keyValueClient.putValue(serverPrefix+name, getPropertiesJSON(name).toString(2));
    }
  }
}
