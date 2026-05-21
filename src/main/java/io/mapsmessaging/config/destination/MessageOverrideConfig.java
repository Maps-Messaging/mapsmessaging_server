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
    this.priority = Priority.valueOf (properties.getProperty("priority", Priority.NORMAL.name()));
    String qos = properties.getProperty("qos", null);
    if(qos != null) {
      this.qualityOfService= QualityOfService.valueOf(qos);
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
    ConfigurationProperties metaConfig = ((ConfigurationProperties)properties.get("meta"));
    if(metaConfig != null) {
      Map<String, Object> preMeta = metaConfig.getMap();
      if (preMeta != null) {
        meta = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : preMeta.entrySet()) {
          meta.put(entry.getKey(), entry.getValue().toString());
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
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("expiry", this.expiry);
    properties.put("priority", this.priority);
    properties.put("qualityOfService", this.qualityOfService);
    properties.put("responseTopic", this.responseTopic);
    properties.put("contentType", this.contentType);
    properties.put("schemaId", this.schemaId);
    properties.put("retain", this.retain);
    properties.put("meta", this.meta);
    properties.put("dataMap", this.dataMap);
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
