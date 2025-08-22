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

package io.mapsmessaging.config.protocol;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.config.ConfigHelper;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;
import io.mapsmessaging.utilities.filtering.NamespaceFilters;

public class LinkConfig extends LinkConfigDTO implements Config {

  public LinkConfig(ConfigurationProperties config) {
    this.direction = config.getProperty("direction");
    this.remoteNamespace = config.getProperty("remote_namespace");
    this.localNamespace = config.getProperty("local_namespace");
    this.selector = config.getProperty("selector");
    this.includeSchema = config.getBooleanProperty("include_schema", false);
    Object obj = config.get("transformer");
    if (obj instanceof ConfigurationProperties tfObj) {
      this.transformer = ConfigHelper.buildMap(tfObj);
    }
    this.namespaceFilters = new NamespaceFilters(config);
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("direction", this.direction);
    config.put("remote_namespace", this.remoteNamespace);
    config.put("local_namespace", this.localNamespace);
    config.put("selector", this.selector);
    config.put("include_schema", this.includeSchema);
    if (transformer != null) {
      config.put("transformer", new ConfigurationProperties(this.transformer));
    }
    if(namespaceFilters != null) {
      config.put("namespaceFilters", namespaceFilters.toConfigurationProperties());
    }
    return config;
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;

    if (config instanceof LinkConfigDTO newConfig) {
      if (this.direction == null || !this.direction.equals(newConfig.getDirection())) {
        this.direction = newConfig.getDirection();
        hasChanged = true;
      }

      if (this.remoteNamespace == null
          || !this.remoteNamespace.equals(newConfig.getRemoteNamespace())) {
        this.remoteNamespace = newConfig.getRemoteNamespace();
        hasChanged = true;
      }

      if (this.localNamespace == null
          || !this.localNamespace.equals(newConfig.getLocalNamespace())) {
        this.localNamespace = newConfig.getLocalNamespace();
        hasChanged = true;
      }

      if (this.selector == null || !this.selector.equals(newConfig.getSelector())) {
        this.selector = newConfig.getSelector();
        hasChanged = true;
      }

      if (this.includeSchema != newConfig.isIncludeSchema()) {
        this.includeSchema = newConfig.isIncludeSchema();
        hasChanged = true;
      }

      if (ConfigHelper.updateMap(this.transformer, newConfig.getTransformer())) {
        hasChanged = true;
      }
    }
    return hasChanged;
  }
}
