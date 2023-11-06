/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.utilities.configuration;


import com.orbitz.consul.ConsulException;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.model.kv.Value;
import io.mapsmessaging.consul.ConsulManagerFactory;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

public class ConsulPropertyManager extends YamlPropertyManager {

  private final String serverPrefix;
  private final Logger logger = LoggerFactory.getLogger(ConsulPropertyManager.class);

  public ConsulPropertyManager(String prefix) {
    if(prefix.startsWith("/")){
      prefix = prefix.substring(1);
    }
    serverPrefix = prefix + "/";
  }

  @Override
  protected void load() {
    try {
      KeyValueClient keyValueClient = ConsulManagerFactory.getInstance().getManager().getKeyValueManager();
      List<String> keys = keyValueClient.getKeys(serverPrefix);
      for (String key : keys) {
        processKey(keyValueClient, key);
      }
    } catch (ConsulException e) {
      logger.log(ServerLogMessages.CONSUL_PROPERTY_MANAGER_NO_KEY_VALUES, serverPrefix);
    }
  }

  private void processKey(KeyValueClient keyValueClient, String key) {
    try {
      Optional<Value> entry = keyValueClient.getValue(key);
      if (entry.isPresent()) {
        Optional<String> optionalValue = entry.get().getValue();
        if(optionalValue.isPresent()){
          String value = new String(Base64.getDecoder().decode(optionalValue.get()));
          String name = key.substring(serverPrefix.length());
          parseAndLoadYaml(name, value);
        }
      }
    } catch (ConsulException | IOException consulException) {
      logger.log(ServerLogMessages.CONSUL_PROPERTY_MANAGER_KEY_LOOKUP_EXCEPTION, key, consulException);
    }
  }

  @Override
  protected void store(String name) {
    logger.log(ServerLogMessages.CONSUL_PROPERTY_MANAGER_STORE, serverPrefix, name);
    KeyValueClient keyValueClient = ConsulManagerFactory.getInstance().getManager().getKeyValueManager();
    keyValueClient.putValue(serverPrefix + name, getPropertiesJSON(name).toString(2));
  }

  @Override
  public void copy(PropertyManager propertyManager) {
    KeyValueClient keyValueClient = ConsulManagerFactory.getInstance().getManager().getKeyValueManager();
    // Remove what we have
    for (String name : properties.keySet()) {
      keyValueClient.deleteKey(serverPrefix + name);
    }

    // Now let's add the new config
    properties.clear();
    properties.putAll(propertyManager.properties.getMap());
    for (String key : properties.keySet()) {
      ConfigurationProperties copy = (ConfigurationProperties) properties.get(key);
      ConfigurationProperties orig = (ConfigurationProperties) propertyManager.properties.get(key);
      copy.setSource(orig.getSource());
    }


    if (properties.getGlobal() != null) {
      properties.getGlobal().clear();
    }
    if (propertyManager.properties.getGlobal() != null) {
      properties.setGlobal(propertyManager.properties.getGlobal());
    }
    properties.setSource(propertyManager.properties.getSource());
    save();
  }

  public void save() {
    logger.log(ServerLogMessages.CONSUL_PROPERTY_MANAGER_SAVE_ALL, serverPrefix);

    KeyValueClient keyValueClient = ConsulManagerFactory.getInstance().getManager().getKeyValueManager();
    for (Entry<String, Object> entry : properties.entrySet()) {
      String source = ((ConfigurationProperties) entry.getValue()).getSource();
      String key = serverPrefix.trim() + entry.getKey().trim();
      keyValueClient.putValue(key, source);
    }
    // Now lets Store it in key value pairs in consul
  }
}
