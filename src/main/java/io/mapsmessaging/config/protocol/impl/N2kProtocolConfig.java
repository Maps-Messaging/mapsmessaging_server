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
import io.mapsmessaging.dto.rest.config.protocol.impl.N2KConfigDTO;
import java.util.Objects;

public class N2kProtocolConfig extends N2KConfigDTO implements Config {

  public N2kProtocolConfig(ConfigurationProperties config) {
    setType("n2k");
    ProtocolConfigFactory.unpack(config, this);

    this.topicNameTemplate = config.getProperty("topicNameTemplate", topicNameTemplate);
    this.parseToJson = config.getBooleanProperty("parseToJson", parseToJson);
    this.databasePath = config.getProperty("databasePath", databasePath);
    this.base64EncodedDatabase = config.getProperty("base64EncodedDatabase", base64EncodedDatabase);
    this.unknownPacketTopic = config.getProperty("unknownPacketTopic", unknownPacketTopic);
    this.canBusAddress = config.getIntProperty("canBusAddress", canBusAddress);
    this.outboundTopicName = config.getProperty("outboundTopicName", outboundTopicName);
    this.qualityOfService = config.getIntProperty("qualityOfService", qualityOfService);
    this.storeOffline = config.getBooleanProperty("storeOffline", storeOffline);
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;

    if (config instanceof N2KConfigDTO newConfig) {
      if (parseToJson != newConfig.isParseToJson()) {
        parseToJson = newConfig.isParseToJson();
        hasChanged = true;
      }

      if (!Objects.equals(topicNameTemplate, newConfig.getTopicNameTemplate())) {
        topicNameTemplate = newConfig.getTopicNameTemplate();
        hasChanged = true;
      }

      if (!Objects.equals(unknownPacketTopic, newConfig.getUnknownPacketTopic())) {
        unknownPacketTopic = newConfig.getUnknownPacketTopic();
        hasChanged = true;
      }

      if (!Objects.equals(outboundTopicName, newConfig.getOutboundTopicName())) {
        outboundTopicName = newConfig.getOutboundTopicName();
        hasChanged = true;
      }

      if (!Objects.equals(databasePath, newConfig.getDatabasePath())) {
        databasePath = newConfig.getDatabasePath();
        hasChanged = true;
      }

      if (!Objects.equals(base64EncodedDatabase, newConfig.getBase64EncodedDatabase())) {
        base64EncodedDatabase = newConfig.getBase64EncodedDatabase();
        hasChanged = true;
      }

      if (canBusAddress != newConfig.getCanBusAddress()) {
        canBusAddress = newConfig.getCanBusAddress();
        hasChanged = true;
      }

      if (qualityOfService != newConfig.getQualityOfService()) {
        qualityOfService = newConfig.getQualityOfService();
        hasChanged = true;
      }

      if (storeOffline != newConfig.isStoreOffline()) {
        storeOffline = newConfig.isStoreOffline();
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
    properties.put("databasePath", databasePath);
    properties.put("base64EncodedDatabase", base64EncodedDatabase);
    properties.put("unknownPacketTopic", unknownPacketTopic);
    properties.put("outboundTopicName", outboundTopicName);
    properties.put("canBusAddress", canBusAddress);
    properties.put("qualityOfService", qualityOfService);
    properties.put("storeOffline", storeOffline);

    return properties;
  }
}