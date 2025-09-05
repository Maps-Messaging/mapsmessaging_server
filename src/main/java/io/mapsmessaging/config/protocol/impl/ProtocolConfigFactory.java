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
import io.mapsmessaging.config.ConfigHelper;
import io.mapsmessaging.config.destination.MessageOverrideConfig;
import io.mapsmessaging.config.protocol.ConnectionAuthConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.MqttConfigDTO;

public class ProtocolConfigFactory {

  private static final String REMOTE_AUTH_CONFIG = "remoteAuthConfig";

  public static void pack(ConfigurationProperties config, ProtocolConfigDTO protocolConfig) {
    config.put("type", protocolConfig.getType());
    if (protocolConfig.getRemoteAuthConfig() != null) {
      config.put(REMOTE_AUTH_CONFIG, ((Config) protocolConfig.getRemoteAuthConfig()).toConfigurationProperties());
    }
    if(protocolConfig.getMessageDefaults() != null) {
      config.put("messageDefaults", protocolConfig.getMessageDefaults());
    }
    if(protocolConfig instanceof MqttConfigDTO){
      MqttConfigDTO mqttConfig = (MqttConfigDTO) protocolConfig;
      config.put("maximumSessionExpiry", mqttConfig.getMaximumSessionExpiry());
      config.put("maximumBufferSize", ConfigHelper.formatBufferSize(mqttConfig.getMaximumBufferSize()));
      config.put("serverReceiveMaximum", mqttConfig.getServerReceiveMaximum());
      config.put("clientReceiveMaximum", mqttConfig.getClientReceiveMaximum());
      config.put("clientMaximumTopicAlias", mqttConfig.getClientMaximumTopicAlias());
      config.put("serverMaximumTopicAlias", mqttConfig.getServerMaximumTopicAlias());
      config.put("strictClientId", mqttConfig.isStrictClientId());
    }
  }

  public static void unpack(ConfigurationProperties config, ProtocolConfigDTO protocolConfigDTO) {
    if (config.getProperty(REMOTE_AUTH_CONFIG) != null) {
      protocolConfigDTO.setRemoteAuthConfig(new ConnectionAuthConfig((ConfigurationProperties) config.get(REMOTE_AUTH_CONFIG)));
    }
    if(config.getProperty("messageDefaults") != null) {
      protocolConfigDTO.setMessageDefaults(new MessageOverrideConfig((ConfigurationProperties) config.get("messageDefaults")));
    }
    if (protocolConfigDTO instanceof MqttConfigDTO) {
      MqttConfig mqttConfig = (MqttConfig) protocolConfigDTO;
      mqttConfig.setMaximumSessionExpiry(config.getLongProperty("maximumSessionExpiry", 86400));
      mqttConfig.setMaximumBufferSize(ConfigHelper.parseBufferSize(config.getProperty("maximumBufferSize", "10M")));
      mqttConfig.setServerReceiveMaximum(config.getIntProperty("serverReceiveMaximum", 10));
      mqttConfig.setClientReceiveMaximum(config.getIntProperty("clientReceiveMaximum", 65535));
      mqttConfig.setClientMaximumTopicAlias(config.getIntProperty("clientMaximumTopicAlias", 32767));
      mqttConfig.setServerMaximumTopicAlias(config.getIntProperty("serverMaximumTopicAlias", 0));
      mqttConfig.setStrictClientId(config.getBooleanProperty("strictClientId", false));
    }
  }

  public static boolean update(ProtocolConfigDTO original, ProtocolConfigDTO updated) {
    boolean hasChanged = false;

    if (original.getRemoteAuthConfig() != null && updated.getRemoteAuthConfig() != null) {
      hasChanged = ((Config) original.getRemoteAuthConfig()).update(updated.getRemoteAuthConfig());
    }
    if(original.getMessageDefaults() != null && updated.getMessageDefaults() != null) {
      hasChanged = ((Config) original.getMessageDefaults()).update(updated.getMessageDefaults());
    }
    if(original instanceof MqttConfig && updated instanceof MqttConfigDTO){
      MqttConfigDTO mqttConfigOriginal = (MqttConfigDTO) original;
      MqttConfigDTO mqttConfigUpdated = (MqttConfigDTO) updated;

      if (mqttConfigOriginal.getMaximumSessionExpiry() != mqttConfigUpdated.getMaximumSessionExpiry()) {
        mqttConfigOriginal.setMaximumSessionExpiry(mqttConfigUpdated.getMaximumSessionExpiry());
        hasChanged = true;
      }
      if (mqttConfigOriginal.getMaximumBufferSize() != mqttConfigUpdated.getMaximumBufferSize()) {
        mqttConfigOriginal.setMaximumBufferSize(mqttConfigUpdated.getMaximumBufferSize());
        hasChanged = true;
      }

      if (mqttConfigOriginal.getServerReceiveMaximum() != mqttConfigUpdated.getServerReceiveMaximum()) {
        mqttConfigOriginal.setServerReceiveMaximum(mqttConfigUpdated.getServerReceiveMaximum());
        hasChanged = true;
      }

      if (mqttConfigOriginal.getClientReceiveMaximum() != mqttConfigUpdated.getClientReceiveMaximum()) {
        mqttConfigOriginal.setClientReceiveMaximum(mqttConfigUpdated.getClientReceiveMaximum());
        hasChanged = true;
      }
      if (mqttConfigOriginal.getClientMaximumTopicAlias() != mqttConfigUpdated.getClientMaximumTopicAlias()) {
        mqttConfigOriginal.setClientMaximumTopicAlias(mqttConfigUpdated.getClientMaximumTopicAlias());
        hasChanged = true;
      }
      if (mqttConfigOriginal.getServerMaximumTopicAlias() != mqttConfigUpdated.getServerMaximumTopicAlias()) {
        mqttConfigOriginal.setServerMaximumTopicAlias(mqttConfigUpdated.getServerMaximumTopicAlias());
        hasChanged = true;
      }
      if (mqttConfigOriginal.isStrictClientId() != mqttConfigUpdated.isStrictClientId()) {
        mqttConfigOriginal.setStrictClientId(mqttConfigUpdated.isStrictClientId());
        hasChanged = true;
      }
    }
    return hasChanged;
  }

  private ProtocolConfigFactory() {}
}
