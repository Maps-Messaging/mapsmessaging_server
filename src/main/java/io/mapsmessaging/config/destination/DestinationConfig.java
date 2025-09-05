/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.config.destination;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.DestinationConfigDTO;
import io.mapsmessaging.license.FeatureManager;

public class DestinationConfig extends DestinationConfigDTO implements Config {

  private static final String OPTIONAL_PATH = "{folder}";

  public DestinationConfig() {

  }

  public DestinationConfig(ConfigurationProperties properties, FeatureManager featureManager) {
    this.directory = properties.getProperty("directory", "");
    this.namespace = properties.getProperty("namespace", "");
    this.type = properties.getProperty("type", "");
    if(!featureManager.isEnabled("storage.fileSupport") && type.equalsIgnoreCase("file")) {
      type = "memory"; // File is not supported
    }
    this.autoPauseTimeout = properties.getIntProperty("autoPauseTimeout", 300);

    storageConfig = ConfigHelper.buildConfig(type, properties);

    if (properties.containsKey("format")) {
      this.format = new FormatConfig((ConfigurationProperties) properties.get("format"));
    }
    if (properties.containsKey("cache") && featureManager.isEnabled("storage.cacheSupport")) {
      this.cache = new CacheConfig((ConfigurationProperties) properties.get("cache"));
    }
    else{
      this.cache = null;
    }
    if(properties.containsKey("messageOverrides")) {
      messageOverride = new MessageOverrideConfig((ConfigurationProperties) properties.get("messageOverrides"));
    }
    String propertyNamespace = this.namespace;
    this.remap =
        (propertyNamespace.endsWith(OPTIONAL_PATH) && this.directory.contains(OPTIONAL_PATH));
    this.namespaceMapping =
        remap
            ? propertyNamespace.substring(0, propertyNamespace.indexOf(OPTIONAL_PATH))
            : propertyNamespace;
  }

  @Override
  public String getTrailingPath() {
    if (remap) {
      return directory.substring(directory.indexOf(OPTIONAL_PATH) + OPTIONAL_PATH.length());
    }
    return "";
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("directory", this.directory);
    properties.put("namespace", this.namespace);
    properties.put("type", this.type);
    properties.put("autoPauseTimeout", this.autoPauseTimeout);
    ConfigHelper.packMap(properties, storageConfig);
    if (this.format != null) {
      properties.put("format", ((Config) format).toConfigurationProperties());
    }
    if (this.cache != null) {
      properties.put("cache", ((Config) cache).toConfigurationProperties());
    }
    if (this.messageOverride != null) {
      properties.put("messageOverride", ((Config) messageOverride).toConfigurationProperties());
    }
    return properties;
  }

  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof DestinationConfigDTO)) {
      return false;
    }

    DestinationConfigDTO newConfig = (DestinationConfigDTO) config;
    boolean hasChanged = false;
    if (this.remap != newConfig.isRemap()) {
      this.remap = newConfig.isRemap();
      hasChanged = true;
    }
    if (!this.directory.equals(newConfig.getDirectory())) {
      this.directory = newConfig.getDirectory();
      hasChanged = true;
    }
    if (this.autoPauseTimeout != newConfig.getAutoPauseTimeout()) {
      this.autoPauseTimeout = newConfig.getAutoPauseTimeout();
      hasChanged = true;
    }
    if (!this.namespace.equals(newConfig.getNamespace())) {
      this.namespace = newConfig.getNamespace();
      hasChanged = true;
    }
    if (!this.type.equals(newConfig.getType())) {
      this.type = newConfig.getType();
      hasChanged = true;
    }

    // Update nested configs and check for changes
    if (this.format != null && ((Config) format).update(newConfig.getFormat())) {
      hasChanged = true;
    }
    if (this.cache != null && ((Config) cache).update(newConfig.getCache())) {
      hasChanged = true;
    }
    if (this.messageOverride != null && ((Config) messageOverride).update(newConfig.getMessageOverride())) {
      hasChanged = true;
    }

    return hasChanged;
  }
}
