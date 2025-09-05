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

package io.mapsmessaging.config.protocol.impl;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.config.protocol.PredefinedTopics;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MqttSnConfigDTO;

import java.util.ArrayList;
import java.util.List;

public class MqttSnConfig extends MqttSnConfigDTO implements Config {

  public MqttSnConfig(ConfigurationProperties config) {
    setType("mqtt-sn");
    ProtocolConfigFactory.unpack(config, this);

    // Initialize MQTT-SN specific fields from config
    this.gatewayId = config.getProperty("gatewayId", gatewayId);
    this.receiveMaximum = config.getIntProperty("receiveMaximum", receiveMaximum);
    this.idleSessionTimeout = config.getLongProperty("idleSessionTimeout", idleSessionTimeout);
    this.maximumSessionExpiry = config.getIntProperty("maximumSessionExpiry", maximumSessionExpiry);
    this.enablePortChanges = config.getBooleanProperty("enablePortChanges", enablePortChanges);
    this.enableAddressChanges = config.getBooleanProperty("enableAddressChanges", enableAddressChanges);
    this.advertiseGateway = config.getBooleanProperty("advertiseGateway", advertiseGateway);
    this.registeredTopics = config.getProperty("registered", registeredTopics);
    this.advertiseInterval = config.getIntProperty("advertiseInterval", advertiseInterval);
    this.maxRegisteredSize = config.getIntProperty("maxRegisteredSize", maxRegisteredSize);
    this.maxInFlightEvents = config.getIntProperty("maxInFlightEvents", maxInFlightEvents);
    this.dropQoS0 = config.getBooleanProperty("dropQoS0Events", dropQoS0);
    this.eventQueueTimeout = config.getIntProperty("eventQueueTimeout", eventQueueTimeout);

    // Initialize predefined topics list
    predefinedTopicsList = new ArrayList<>();
    Object predefined = config.get("preDefinedTopics");
    if (predefined instanceof List) {
      for (ConfigurationProperties props : (List<ConfigurationProperties>) predefined) {
        predefinedTopicsList.add(new PredefinedTopics(props));
      }
    }
  }

  @Override
  public boolean update(BaseConfigDTO config) {
    boolean hasChanged = false;
    if (config instanceof MqttSnConfigDTO) {
      MqttSnConfigDTO newConfig = (MqttSnConfigDTO) config;

      // Check each field and update if necessary
      if (!this.gatewayId.equals(newConfig.getGatewayId())) {
        this.gatewayId = newConfig.getGatewayId();
        hasChanged = true;
      }
      if (this.receiveMaximum != newConfig.getReceiveMaximum()) {
        this.receiveMaximum = newConfig.getReceiveMaximum();
        hasChanged = true;
      }
      if (this.idleSessionTimeout != newConfig.getIdleSessionTimeout()) {
        this.idleSessionTimeout = newConfig.getIdleSessionTimeout();
        hasChanged = true;
      }
      if (this.maximumSessionExpiry != newConfig.getMaximumSessionExpiry()) {
        this.maximumSessionExpiry = newConfig.getMaximumSessionExpiry();
        hasChanged = true;
      }
      if (this.enablePortChanges != newConfig.isEnablePortChanges()) {
        this.enablePortChanges = newConfig.isEnablePortChanges();
        hasChanged = true;
      }
      if (this.enableAddressChanges != newConfig.isEnableAddressChanges()) {
        this.enableAddressChanges = newConfig.isEnableAddressChanges();
        hasChanged = true;
      }
      if (this.advertiseGateway != newConfig.isAdvertiseGateway()) {
        this.advertiseGateway = newConfig.isAdvertiseGateway();
        hasChanged = true;
      }
      if (!this.registeredTopics.equals(newConfig.getRegisteredTopics())) {
        this.registeredTopics = newConfig.getRegisteredTopics();
        hasChanged = true;
      }
      if (this.advertiseInterval != newConfig.getAdvertiseInterval()) {
        this.advertiseInterval = newConfig.getAdvertiseInterval();
        hasChanged = true;
      }
      if (this.maxRegisteredSize != newConfig.getMaxRegisteredSize()) {
        this.maxRegisteredSize = newConfig.getMaxRegisteredSize();
        hasChanged = true;
      }
      if (this.maxInFlightEvents != newConfig.getMaxInFlightEvents()) {
        this.maxInFlightEvents = newConfig.getMaxInFlightEvents();
        hasChanged = true;
      }
      if (this.dropQoS0 != newConfig.isDropQoS0()) {
        this.dropQoS0 = newConfig.isDropQoS0();
        hasChanged = true;
      }
      if (this.eventQueueTimeout != newConfig.getEventQueueTimeout()) {
        this.eventQueueTimeout = newConfig.getEventQueueTimeout();
        hasChanged = true;
      }

      // Update predefined topics list
      if (!this.predefinedTopicsList.equals(newConfig.getPredefinedTopicsList())) {
        this.predefinedTopicsList = newConfig.getPredefinedTopicsList();
        hasChanged = true;
      }

      // Update fields from ProtocolConfigFactory if needed
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
    properties.put("gatewayId", this.gatewayId);
    properties.put("receiveMaximum", this.receiveMaximum);
    properties.put("idleSessionTimeout", this.idleSessionTimeout);
    properties.put("maximumSessionExpiry", this.maximumSessionExpiry);
    properties.put("enablePortChanges", this.enablePortChanges);
    properties.put("enableAddressChanges", this.enableAddressChanges);
    properties.put("advertiseGateway", this.advertiseGateway);
    properties.put("registered", this.registeredTopics);
    properties.put("advertiseInterval", this.advertiseInterval);
    properties.put("maxRegisteredSize", this.maxRegisteredSize);
    properties.put("maxInFlightEvents", this.maxInFlightEvents);
    properties.put("dropQoS0Events", this.dropQoS0);
    properties.put("eventQueueTimeout", this.eventQueueTimeout);

    // Add predefined topics list to properties
    List<ConfigurationProperties> predefinedPropsList = new ArrayList<>();
    for (PredefinedTopics topic : predefinedTopicsList) {
      predefinedPropsList.add(topic.toConfigurationProperties());
    }
    properties.put("preDefinedTopics", predefinedPropsList);

    return properties;
  }
}
