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

package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.CanAerospaceConfigDTO;

public class CanAerospaceProtocolConfig extends CanAerospaceConfigDTO implements Config {

  public CanAerospaceProtocolConfig(ConfigurationProperties config) {
    setType("canaerospace");
    ProtocolConfigFactory.unpack(config, this);

    this.topicNameTemplate = config.getProperty("topicNameTemplate", topicNameTemplate);
    this.parseToJson = config.getBooleanProperty("parseToJson", parseToJson);
    this.yamlPath = config.getProperty("yamlPath", yamlPath);
    this.unknownPacketTopic = config.getProperty("unknownPacketTopic", unknownPacketTopic);
    this.inboundTopicName = config.getProperty("inboundTopicName", inboundTopicName);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;

    if (config instanceof CanAerospaceConfigDTO newConfig) {

      if (parseToJson != newConfig.isParseToJson()) {
        parseToJson = newConfig.isParseToJson();
        hasChanged = true;
      }

      if (!topicNameTemplate.equals(newConfig.getTopicNameTemplate())) {
        topicNameTemplate = newConfig.getTopicNameTemplate();
        hasChanged = true;
      }

      if (!unknownPacketTopic.equals(newConfig.getUnknownPacketTopic())) {
        unknownPacketTopic = newConfig.getUnknownPacketTopic();
        hasChanged = true;
      }

      if (!equalsNullable(yamlPath, newConfig.getYamlPath())) {
        yamlPath = newConfig.getYamlPath();
        hasChanged = true;
      }

      if (!equalsNullable(inboundTopicName, newConfig.getInboundTopicName())) {
        inboundTopicName = newConfig.getInboundTopicName();
        hasChanged = true;
      }

      if (ProtocolConfigFactory.update(this, newConfig)) {
        hasChanged = true;
      }
    }

    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    ProtocolConfigFactory.pack(properties, this);

    properties.put("topicNameTemplate", topicNameTemplate);
    properties.put("parseToJson", parseToJson);
    properties.put("unknownPacketTopic", unknownPacketTopic);

    if (yamlPath != null && !yamlPath.isEmpty()) {
      properties.put("yamlPath", yamlPath);
    }

    if (inboundTopicName != null && !inboundTopicName.isEmpty()) {
      properties.put("inboundTopicName", inboundTopicName);
    }

    return properties;
  }

  private boolean equalsNullable(String currentValue, String newValue) {
    if (currentValue == null) {
      return newValue == null;
    }
    return currentValue.equals(newValue);
  }
}