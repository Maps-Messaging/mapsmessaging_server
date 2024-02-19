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

package io.mapsmessaging.utilities.configuration;


import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.consul.ConsulManagerFactory;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import static io.mapsmessaging.logging.ServerLogMessages.CONSUL_PROPERTY_MANAGER_KEY_LOOKUP_SUCCESS;

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
  public void load() {
    try {
      List<String> keys = ConsulManagerFactory.getInstance().getManager().getKeys(serverPrefix);
      for (String key : keys) {
        processKey(key);
      }
    } catch (IOException e) {
      logger.log(ServerLogMessages.CONSUL_PROPERTY_MANAGER_NO_KEY_VALUES, serverPrefix);
    }
  }

  private void processKey(String key) {
    try {
      String value = ConsulManagerFactory.getInstance().getManager().getValue(key);
      String name = key.substring(serverPrefix.length());
      logger.log(CONSUL_PROPERTY_MANAGER_KEY_LOOKUP_SUCCESS, name, value.length());
      parseAndLoadYaml(name, value);
    } catch (IOException consulException) {
      logger.log(ServerLogMessages.CONSUL_PROPERTY_MANAGER_KEY_LOOKUP_EXCEPTION, key, consulException);
    }
  }

  @Override
  protected void store(String name) throws IOException {
    logger.log(ServerLogMessages.CONSUL_PROPERTY_MANAGER_STORE, serverPrefix, name);
    ConsulManagerFactory.getInstance()
        .getManager()
        .putValue(serverPrefix + name, getPropertiesJSON(name).toString(2));
  }

  @Override
  public void copy(PropertyManager propertyManager) throws IOException {
    // Remove what we have
    for (String name : properties.keySet()) {
      ConsulManagerFactory.getInstance()
          .getManager()
          .deleteKey(serverPrefix + name);
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

  public void save() throws IOException {
    logger.log(ServerLogMessages.CONSUL_PROPERTY_MANAGER_SAVE_ALL, serverPrefix);
    for (Entry<String, Object> entry : properties.entrySet()) {
      String source = ((ConfigurationProperties) entry.getValue()).getSource();
      String key = serverPrefix.trim() + entry.getKey().trim();
      ConsulManagerFactory.getInstance().getManager().putValue(key, source);
    }
  }
}
