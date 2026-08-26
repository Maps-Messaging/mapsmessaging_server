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

import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.destination.MessageOverrideDTO;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class MessageOverrideConfig extends MessageOverrideDTO implements Config {

  public MessageOverrideConfig(ConfigurationProperties properties) {
    this.expiry = properties.getLongProperty("expiry", -1);
    String configuredPriority = properties.getProperty("priority", null);
    if (configuredPriority != null) {
      this.priority = Priority.valueOf(configuredPriority);
    }
    String qos = properties.getProperty("qos", properties.getProperty("qualityOfService", null));
    if (qos != null) {
      this.qualityOfService = QualityOfService.valueOf(qos);
    }
    this.responseTopic = properties.getProperty("responseTopic", null);
    this.contentType = properties.getProperty("contentType", null);
    this.schemaId = properties.getProperty("schemaId", null);
    if(properties.containsKey("retain")) {
      this.retain = properties.getBooleanProperty("retain",false);
    }
    else{
      this.retain = null;
    }
    if (properties.containsKey("storeOffline")) {
      this.storeOffline = properties.getBooleanProperty("storeOffline", false);
    }
    else{
      this.storeOffline = null;
    }
    ConfigurationProperties metaConfig = ((ConfigurationProperties)properties.get("meta"));
    if(metaConfig != null) {
      Map<String, Object> preMeta = metaConfig.getMap();
      if (preMeta != null) {
        meta = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : preMeta.entrySet()) {
          if (entry.getValue() != null) {
            meta.put(entry.getKey(), entry.getValue().toString());
          }
        }
      }
    }
    ConfigurationProperties dataMapConfig = ((ConfigurationProperties)properties.get("dataMap"));
    if(dataMapConfig != null) {
      dataMap = dataMapConfig.getMap();
    }
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    return toConfigurationProperties(this);
  }

  public static ConfigurationProperties toConfigurationProperties(MessageOverrideDTO config) {
    ConfigurationProperties properties = new ConfigurationProperties();
    if (config.getExpiry() != null && config.getExpiry() >= 0) {
      properties.put("expiry", config.getExpiry());
    }
    if (config.getPriority() != null) {
      properties.put("priority", config.getPriority());
    }
    if (config.getQualityOfService() != null) {
      properties.put("qos", config.getQualityOfService());
    }
    if (config.getResponseTopic() != null) {
      properties.put("responseTopic", config.getResponseTopic());
    }
    if (config.getContentType() != null) {
      properties.put("contentType", config.getContentType());
    }
    if (config.getSchemaId() != null) {
      properties.put("schemaId", config.getSchemaId());
    }
    if (config.getRetain() != null) {
      properties.put("retain", config.getRetain());
    }
    if (config.getStoreOffline() != null) {
      properties.put("storeOffline", config.getStoreOffline());
    }
    if (config.getMeta() != null) {
      properties.put("meta", new ConfigurationProperties(new LinkedHashMap<>(config.getMeta())));
    }
    if (config.getDataMap() != null) {
      properties.put("dataMap", new ConfigurationProperties(new LinkedHashMap<>(config.getDataMap())));
    }
    return properties;
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    if (!(config instanceof MessageOverrideDTO)) {
      return false;
    }

    MessageOverrideDTO newConfig = (MessageOverrideDTO) config;
    boolean hasChanged = false;

    if (!Objects.equals(this.expiry, newConfig.getExpiry())) {
      this.expiry = newConfig.getExpiry();
      hasChanged = true;
    }
    if (!Objects.equals(this.priority, newConfig.getPriority())) {
      this.priority = newConfig.getPriority();
      hasChanged = true;
    }
    if (!Objects.equals(this.qualityOfService, newConfig.getQualityOfService())) {
      this.qualityOfService = newConfig.getQualityOfService();
      hasChanged = true;
    }
    if (!Objects.equals(this.responseTopic, newConfig.getResponseTopic())) {
      this.responseTopic = newConfig.getResponseTopic();
      hasChanged = true;
    }
    if (!Objects.equals(this.contentType, newConfig.getContentType())) {
      this.contentType = newConfig.getContentType();
      hasChanged = true;
    }
    if (!Objects.equals(this.schemaId, newConfig.getSchemaId())) {
      this.schemaId = newConfig.getSchemaId();
      hasChanged = true;
    }
    if (!Objects.equals(this.retain, newConfig.getRetain())) {
      this.retain = newConfig.getRetain();
      hasChanged = true;
    }
    if (!Objects.equals(this.storeOffline, newConfig.getStoreOffline())) {
      this.storeOffline = newConfig.getStoreOffline();
      hasChanged = true;
    }
    if (!Objects.equals(this.meta, newConfig.getMeta())) {
      this.meta = newConfig.getMeta();
      hasChanged = true;
    }
    if (!Objects.equals(this.dataMap, newConfig.getDataMap())) {
      this.dataMap = newConfig.getDataMap();
      hasChanged = true;
    }

    return hasChanged;
  }
}
