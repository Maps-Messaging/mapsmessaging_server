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
import io.mapsmessaging.config.protocol.impl.n2k.N2KAisConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.N2KConfigDTO;

public class N2kProtocolConfig extends N2KConfigDTO implements Config {

  public N2kProtocolConfig(ConfigurationProperties config) {
    setType("n2k");
    ProtocolConfigFactory.unpack(config, this);

    this.topicNameTemplate = config.getProperty("topicNameTemplate", topicNameTemplate);
    this.parseToJson = config.getBooleanProperty("parseToJson", parseToJson);
    this.databasePath = config.getProperty("databasePath", databasePath);
    this.base64EncodedDatabase = config.getProperty("base64EncodedDatabase", base64EncodedDatabase);
    this.unknownPacketTopic = config.getProperty("unknownPacketTopic", unknownPacketTopic);
    this.inboundTopicName = config.getProperty("inboundTopicName", inboundTopicName);
    this.publishMavlinkDrones = config.getBooleanProperty("publishMavlinkDrones", publishMavlinkDrones);
    this.canBusAddress = config.getIntProperty("canBusAddress", canBusAddress);

    if (config.containsKey("ais")) {
      ConfigurationProperties aisProperties = (ConfigurationProperties) config.get("ais");
      this.ais = new N2KAisConfig(aisProperties);
    }
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;

    if (config instanceof N2KConfigDTO newConfig) {
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

      if (databasePath == null && newConfig.getDatabasePath() != null) {
        databasePath = newConfig.getDatabasePath();
        hasChanged = true;
      } else if (databasePath != null && !databasePath.equals(newConfig.getDatabasePath())) {
        databasePath = newConfig.getDatabasePath();
        hasChanged = true;
      }

      if (base64EncodedDatabase == null && newConfig.getBase64EncodedDatabase() != null) {
        base64EncodedDatabase = newConfig.getBase64EncodedDatabase();
        hasChanged = true;
      } else if (base64EncodedDatabase != null && !base64EncodedDatabase.equals(newConfig.getBase64EncodedDatabase())) {
        base64EncodedDatabase = newConfig.getBase64EncodedDatabase();
        hasChanged = true;
      }

      if (inboundTopicName == null && newConfig.getInboundTopicName() != null) {
        inboundTopicName = newConfig.getInboundTopicName();
        hasChanged = true;
      } else if (inboundTopicName != null && !inboundTopicName.equals(newConfig.getInboundTopicName())) {
        inboundTopicName = newConfig.getInboundTopicName();
        hasChanged = true;
      }

      if (publishMavlinkDrones != newConfig.isPublishMavlinkDrones()) {
        publishMavlinkDrones = newConfig.isPublishMavlinkDrones();
        hasChanged = true;
      }

      if (canBusAddress != newConfig.getCanBusAddress()) {
        canBusAddress = newConfig.getCanBusAddress();
        hasChanged = true;
      }

      if (ais == null && newConfig.getAis() != null) {
        ais = newConfig.getAis();
        hasChanged = true;
      } else if (ais != null && !ais.equals(newConfig.getAis())) {
        ais = newConfig.getAis();
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
    properties.put("inboundTopicName", inboundTopicName);
    properties.put("publishMavlinkDrones", publishMavlinkDrones);
    properties.put("canBusAddress", canBusAddress);

    if (ais instanceof Config aisConfiguration) {
      properties.put("ais", aisConfiguration.toConfigurationProperties());
    }

    return properties;
  }
}