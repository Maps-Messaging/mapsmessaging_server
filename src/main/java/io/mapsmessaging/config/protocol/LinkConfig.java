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

package io.mapsmessaging.config.protocol;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.config.Config;
import io.mapsmessaging.config.analytics.StatisticsConfig;
import io.mapsmessaging.config.transformer.TransformationConfigFactory;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.NamespaceFilterDTO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;


public class LinkConfig extends LinkConfigDTO implements Config {

  public LinkConfig(ConfigurationProperties config) {
    this.direction = config.getProperty("direction");
    this.remoteNamespace = config.getProperty("remote_namespace");
    this.localNamespace = config.getProperty("local_namespace");
    this.selector = config.getProperty("selector");
    int qos = config.getIntProperty("qos", 0);
    qualityOfService = QualityOfService.getInstance(qos % 3);
    this.includeSchema = config.getBooleanProperty("include_schema", false);
    Object obj = config.get("transformer");
    if (obj != null) {
      transformer = TransformationConfigFactory.loadChain(obj);
    }
    this.namespaceFilters = new ArrayList<>();
    Object namespaceFilterConfig = config.get("namespaceFilters");
    if (namespaceFilterConfig instanceof List<?> list) {
      this.namespaceFilters = new ArrayList<>();
      for (Object entry : list) {
        if (entry instanceof ConfigurationProperties prop) {
          this.namespaceFilters.add(new NamespaceFilter(prop));
        }
      }
    }
    else if (namespaceFilterConfig instanceof ConfigurationProperties prop) {
      this.namespaceFilters = new ArrayList<>();
      this.namespaceFilters.add(new NamespaceFilter(prop));
    }
    else {
      this.namespaceFilters = new ArrayList<>();
    }

    Object analysticsConfig = config.get("analystics");
    if (analysticsConfig instanceof ConfigurationProperties tfObj) {
      statistics = new StatisticsConfig(tfObj);
    }
    else{
      statistics = null;
    }
    if(config.containsKey("linkProperties") && config.get("linkProperties") instanceof ConfigurationProperties props){
      this.linkProperties = props.getMap();
    }
    else{
      this.linkProperties = new LinkedHashMap<>();
    }
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties config = new ConfigurationProperties();
    config.put("direction", this.direction);
    config.put("remote_namespace", this.remoteNamespace);
    config.put("local_namespace", this.localNamespace);
    config.put("selector", this.selector);
    config.put("include_schema", this.includeSchema);

    if (this.qualityOfService != null) {
      config.put("qos", this.qualityOfService.getLevel());
    }

    if (this.transformer != null && !this.transformer.isEmpty()) {
      config.put("transformer", this.transformer);
    }

    if (this.namespaceFilters != null && !this.namespaceFilters.isEmpty()) {
      List<ConfigurationProperties> configList = new ArrayList<>();
      for (NamespaceFilterDTO filter : namespaceFilters) {
        configList.add(((NamespaceFilter) filter).toConfigurationProperties());
      }
      config.put("namespaceFilters", configList);
    }

    if (this.statistics != null) {
      config.put("analystics", ((StatisticsConfig) this.statistics).toConfigurationProperties());
    }

    if (this.linkProperties != null && !this.linkProperties.isEmpty()) {
      config.put("linkProperties", this.linkProperties);
    }

    return config;
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;

    if (config instanceof LinkConfigDTO newConfig) {
      if (!java.util.Objects.equals(this.direction, newConfig.getDirection())) {
        this.direction = newConfig.getDirection();
        hasChanged = true;
      }

      if (!java.util.Objects.equals(this.remoteNamespace, newConfig.getRemoteNamespace())) {
        this.remoteNamespace = newConfig.getRemoteNamespace();
        hasChanged = true;
      }

      if (!java.util.Objects.equals(this.localNamespace, newConfig.getLocalNamespace())) {
        this.localNamespace = newConfig.getLocalNamespace();
        hasChanged = true;
      }

      if (!java.util.Objects.equals(this.selector, newConfig.getSelector())) {
        this.selector = newConfig.getSelector();
        hasChanged = true;
      }

      if (this.includeSchema != newConfig.isIncludeSchema()) {
        this.includeSchema = newConfig.isIncludeSchema();
        hasChanged = true;
      }

      if (!java.util.Objects.equals(this.qualityOfService, newConfig.getQualityOfService())) {
        this.qualityOfService = newConfig.getQualityOfService();
        hasChanged = true;
      }

      if (!java.util.Objects.equals(this.transformer, newConfig.getTransformer())) {
        this.transformer =
            newConfig.getTransformer() != null
                ? new ArrayList<>(newConfig.getTransformer())
                : null;
        hasChanged = true;
      }

      if (!java.util.Objects.equals(this.namespaceFilters, newConfig.getNamespaceFilters())) {
        this.namespaceFilters =
            newConfig.getNamespaceFilters() != null
                ? new ArrayList<>(newConfig.getNamespaceFilters())
                : null;
        hasChanged = true;
      }

      if (!java.util.Objects.equals(this.linkProperties, newConfig.getLinkProperties())) {
        this.linkProperties =
            newConfig.getLinkProperties() != null
                ? new LinkedHashMap<>(newConfig.getLinkProperties())
                : new LinkedHashMap<>();
        hasChanged = true;
      }

      if (!java.util.Objects.equals(this.statistics, newConfig.getStatistics())) {
        this.statistics = newConfig.getStatistics();
        hasChanged = true;
      }
    }
    return hasChanged;
  }
}
