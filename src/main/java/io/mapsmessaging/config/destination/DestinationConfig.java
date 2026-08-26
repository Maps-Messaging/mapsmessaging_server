/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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
import io.mapsmessaging.dto.rest.config.destination.CacheConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.DestinationConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.FormatConfigDTO;
import io.mapsmessaging.license.FeatureManager;

import java.util.Objects;

public class DestinationConfig extends DestinationConfigDTO implements Config {

  private static final String OPTIONAL_PATH = "{folder}";

  public DestinationConfig() {

  }

  public DestinationConfig(ConfigurationProperties properties, FeatureManager featureManager) {
    this.directory = properties.getProperty("directory", directory);
    this.namespace = properties.getProperty("namespace", namespace);
    this.type = properties.getProperty("type", "");
    if(!featureManager.isEnabled("storage.fileSupport")
        && type.equalsIgnoreCase("file")) {
      type = "memory"; // File is not supported
    }
    this.autoPauseTimeout = properties.getIntProperty("autoPauseTimeout", autoPauseTimeout);

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
    if (properties.containsKey("messageOverrides")) {
      messageOverride = new MessageOverrideConfig((ConfigurationProperties) properties.get("messageOverrides"));
    }
    else if (properties.containsKey("messageOverride")) {
      messageOverride = new MessageOverrideConfig((ConfigurationProperties) properties.get("messageOverride"));
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
    return toConfigurationProperties(this);
  }

  public static ConfigurationProperties toConfigurationProperties(DestinationConfigDTO config) {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("directory", config.getDirectory());
    properties.put("namespace", config.getNamespace());
    properties.put("type", config.getType());
    properties.put("autoPauseTimeout", config.getAutoPauseTimeout());
    ConfigHelper.packMap(properties, config.getStorageConfig());
    if (config.getFormat() != null) {
      ConfigurationProperties formatProperties = new ConfigurationProperties();
      formatProperties.put("name", config.getFormat().getName());
      properties.put("format", formatProperties);
    }
    if (config.getCache() != null) {
      ConfigurationProperties cacheProperties = new ConfigurationProperties();
      cacheProperties.put("type", config.getCache().getType());
      cacheProperties.put("writeThrough", config.getCache().isWriteThrough() ? "enable" : "disable");
      properties.put("cache", cacheProperties);
    }
    if (config.getMessageOverride() != null) {
      properties.put("messageOverrides", MessageOverrideConfig.toConfigurationProperties(config.getMessageOverride()));
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

    if (!Objects.equals(this.storageConfig, newConfig.getStorageConfig())) {
      this.storageConfig = newConfig.getStorageConfig();
      hasChanged = true;
    }

    // Update nested configs and check for changes
    if (this.format instanceof Config formatConfig && newConfig.getFormat() != null) {
      hasChanged |= formatConfig.update(newConfig.getFormat());
    }
    else if (!Objects.equals(this.format, newConfig.getFormat())) {
      this.format = copyFormat(newConfig.getFormat());
      hasChanged = true;
    }
    if (this.cache instanceof Config cacheConfig && newConfig.getCache() != null) {
      hasChanged |= cacheConfig.update(newConfig.getCache());
    }
    else if (!Objects.equals(this.cache, newConfig.getCache())) {
      this.cache = copyCache(newConfig.getCache());
      hasChanged = true;
    }
    if (this.messageOverride instanceof Config overrideConfig && newConfig.getMessageOverride() != null) {
      hasChanged |= overrideConfig.update(newConfig.getMessageOverride());
    }
    else if (!Objects.equals(this.messageOverride, newConfig.getMessageOverride())) {
      this.messageOverride = newConfig.getMessageOverride() == null
          ? null
          : new MessageOverrideConfig(MessageOverrideConfig.toConfigurationProperties(newConfig.getMessageOverride()));
      hasChanged = true;
    }

    String propertyNamespace = this.namespace;
    this.remap = propertyNamespace.endsWith(OPTIONAL_PATH) && this.directory.contains(OPTIONAL_PATH);
    this.namespaceMapping = remap
        ? propertyNamespace.substring(0, propertyNamespace.indexOf(OPTIONAL_PATH))
        : propertyNamespace;

    return hasChanged;
  }

  private FormatConfigDTO copyFormat(FormatConfigDTO config) {
    if (config == null) {
      return null;
    }
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("name", config.getName());
    return new FormatConfig(properties);
  }

  private CacheConfigDTO copyCache(CacheConfigDTO config) {
    if (config == null) {
      return null;
    }
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("type", config.getType());
    properties.put("writeThrough", config.isWriteThrough() ? "enable" : "disable");
    return new CacheConfig(properties);
  }
}
