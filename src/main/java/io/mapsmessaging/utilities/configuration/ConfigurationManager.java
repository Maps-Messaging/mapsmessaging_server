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

import io.mapsmessaging.consul.ConsulManagerFactory;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class ConfigurationManager {

  private static final ConfigurationManager instance;

  static {
    instance = new ConfigurationManager();
  }

  private final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

  private final List<PropertyManager> propertyManagers;
  private PropertyManager authoritative;

  private ConfigurationManager() {
    logger.log(ServerLogMessages.PROPERTY_MANAGER_START);
    propertyManagers = new ArrayList<>();
    authoritative = null;
  }

  public static ConfigurationManager getInstance() {
    return instance;
  }

  public void initialise(@NonNull @NotNull String serverId) {
    ConsulPropertyManager defaultConsulManager = null;
    if (ConsulManagerFactory.getInstance().isStarted()) {
      authoritative = new ConsulPropertyManager(serverId);
      authoritative.load();
      defaultConsulManager = new ConsulPropertyManager("default");
      defaultConsulManager.load();
      propertyManagers.add(defaultConsulManager);
    }
    YamlPropertyManager yamlPropertyManager = new FileYamlPropertyManager();
    propertyManagers.add(yamlPropertyManager);
    for (PropertyManager manager : propertyManagers) {
      manager.load();
    }

    // We have a consul link but there is no config loaded, so load up the configuration into the
    // consul server to bootstrap the server
    if (defaultConsulManager != null && defaultConsulManager.properties.size() == 0) {
      defaultConsulManager.copy(yamlPropertyManager);
    }
    if(authoritative != null && authoritative.properties.size() == 0){
      authoritative.copy(defaultConsulManager); // Define the local host
    }
  }

  public @NonNull @NotNull ConfigurationProperties getProperties(String name) {
    ConfigurationProperties config;
    if (authoritative != null && authoritative.contains(name)) {
      return authoritative.getProperties(name);
    }

    for (PropertyManager manager : propertyManagers) {
      if ( manager.contains(name)) {
        return manager.getProperties(name);
      }
    }
    return new ConfigurationProperties(new HashMap<>());
  }
}
