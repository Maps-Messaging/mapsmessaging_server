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
import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.N2KConfigDTO;

public class N2kProtocolConfig extends N2KConfigDTO implements Config {

  public N2kProtocolConfig(ConfigurationProperties config) {
    setType("n2k");
    ProtocolConfigFactory.unpack(config, this);

    this.topicNameTemplate = config.getProperty("topicNameTemplate", topicNameTemplate);
    this.parseToJson = config.getBooleanProperty("parseToJson", parseToJson);
    this.databasePath= config.getProperty("databasePath", databasePath);
    this.unknownPacketTopic = config.getProperty("unknownPacketTopic", unknownPacketTopic);
    this.publishMavlinkDrones = config.getBooleanProperty("publishMavlinkDrones", publishMavlinkDrones);
    this.canBusAddress = config.getIntProperty("canBusAddress", canBusAddress);
    this.modelId = config.getProperty("modelId", "Maps Messaging Server");
    this.modelSerialCode = config.getProperty("modelSerialCode", "unknown");
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof MavlinkConfigDTO newConfig) {

      if(parseToJson != newConfig.isParseToJson()){
        parseToJson = newConfig.isParseToJson();
        hasChanged = true;
      }
      if(!topicNameTemplate.equals(newConfig.getTopicNameTemplate())) {
        this.topicNameTemplate = newConfig.getTopicNameTemplate();
        hasChanged = true;
      }
      // Update fields from ProtocolConfigFactory if needed
      if (ProtocolConfigFactory.update(this, newConfig)) {
        hasChanged = true;
      }
      if(this.canBusAddress != canBusAddress){
        this.canBusAddress = canBusAddress;
        hasChanged = true;
      }
    }
    return hasChanged;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    ProtocolConfigFactory.pack(properties, this);
    properties.put("topicNameTemplate", this.topicNameTemplate);
    properties.put("parseToJson", this.parseToJson);
    properties.put("databasePath", this.databasePath);
    properties.put("unknownPacketTopic", this.unknownPacketTopic);
    properties.put("publishMavlinkDrones", this.publishMavlinkDrones);
    properties.put("canBusAddress", this.canBusAddress);
    properties.put("modelId", this.modelId);
    properties.put("modelSerialCode", this.modelSerialCode);
    return properties;
  }
}
