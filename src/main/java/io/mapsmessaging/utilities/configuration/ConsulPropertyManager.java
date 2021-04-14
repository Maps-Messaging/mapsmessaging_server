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


import com.orbitz.consul.ConsulException;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.model.kv.Value;
import io.mapsmessaging.consul.ConsulManagerFactory;
import io.mapsmessaging.logging.LogMessages;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.json.JSONException;
import org.json.JSONObject;

public class ConsulPropertyManager extends PropertyManager {

  private final String serverPrefix;
  private final Logger logger = LoggerFactory.getLogger(ConsulPropertyManager.class);

  public ConsulPropertyManager(String prefix){
    serverPrefix = prefix+"_";
  }

  protected void load() {
    try {
      KeyValueClient keyValueClient = ConsulManagerFactory.getInstance().getManager().getKeyValueManager();
      List<String> keys = keyValueClient.getKeys(serverPrefix);
      for (String key : keys) {
        processKey(keyValueClient, key);
      }
    } catch (ConsulException e) {
      logger.log(LogMessages.CONSUL_PROPERTY_MANAGER_NO_KEY_VALUES, serverPrefix);
    }
  }

  private void processKey(KeyValueClient keyValueClient, String key){
    try {
      Optional<Value> entry = keyValueClient.getValue(key);
      if (entry.isPresent()) {
        Optional<String> optionalValue = entry.get().getValue();
        optionalValue.ifPresent(s -> {
          String value = s;
          value = new String(Base64.getDecoder().decode(value));
          loadPropertiesJSON(key.substring(serverPrefix.length()), new JSONObject(value));
        });
      }
    }
    catch(ConsulException consulException){
      logger.log(LogMessages.CONSUL_PROPERTY_MANAGER_KEY_LOOKUP_EXCEPTION, key, consulException);
    }
    catch(JSONException jsonException){
      jsonException.printStackTrace();
      logger.log(LogMessages.CONSUL_PROPERTY_MANAGER_INVALID_JSON, key, jsonException);
    }
  }

  @Override
  protected void store(String name) {
    logger.log(LogMessages.CONSUL_PROPERTY_MANAGER_STORE, serverPrefix, name);
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
    logger.log(LogMessages.CONSUL_PROPERTY_MANAGER_SAVE_ALL, serverPrefix);

    KeyValueClient keyValueClient = ConsulManagerFactory.getInstance().getManager().getKeyValueManager();
    // Now lets Store it in key value pairs in consul
    for(String name:properties.keySet()){
      keyValueClient.putValue(serverPrefix+name, getPropertiesJSON(name).toString(2));
    }
  }
}
