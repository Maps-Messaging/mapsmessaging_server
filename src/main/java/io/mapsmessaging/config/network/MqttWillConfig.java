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

package io.mapsmessaging.config.network;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.MqttWillConfigDTO;

import java.util.Objects;

public class MqttWillConfig extends MqttWillConfigDTO implements Config {

  public MqttWillConfig() {
    super();
  }

  public MqttWillConfig(ConfigurationProperties properties) {
    this.topic = properties.getProperty("topic", null);
    this.payload = properties.getProperty("payload", payload);
    this.payloadEncoding = properties.getProperty("payloadEncoding", payloadEncoding);
    this.qos = properties.getIntProperty("qos", qos);
    this.retain = properties.getBooleanProperty("retain", retain);
    this.delayInterval = properties.getLongProperty("delayInterval", delayInterval);
    this.messageExpiryInterval = properties.getLongProperty("messageExpiryInterval", messageExpiryInterval);
    this.contentType = properties.getProperty("contentType", contentType);
    this.payloadFormatIndicator = properties.getIntProperty("payloadFormatIndicator", payloadFormatIndicator);
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties configurationProperties = new ConfigurationProperties();
    configurationProperties.put("topic", topic);
    configurationProperties.put("payload", payload);
    configurationProperties.put("payloadEncoding", payloadEncoding);
    configurationProperties.put("qos", qos);
    configurationProperties.put("retain", retain);
    configurationProperties.put("delayInterval", delayInterval);
    configurationProperties.put("messageExpiryInterval", messageExpiryInterval);
    configurationProperties.put("contentType", contentType);
    configurationProperties.put("payloadFormatIndicator", payloadFormatIndicator);
    return configurationProperties;
  }

  @Override
  public boolean update(BaseConfigDTO update) {
    boolean hasChanged = false;

    if (update instanceof MqttWillConfigDTO mqttWillConfigDTO) {
      if (!Objects.equals(topic, mqttWillConfigDTO.getTopic())) {
        topic = mqttWillConfigDTO.getTopic();
        hasChanged = true;
      }

      if (!Objects.equals(payload, mqttWillConfigDTO.getPayload())) {
        payload = mqttWillConfigDTO.getPayload();
        hasChanged = true;
      }

      if (!Objects.equals(payloadEncoding, mqttWillConfigDTO.getPayload())) {
        payloadEncoding = mqttWillConfigDTO.getPayload();
        hasChanged = true;
      }
      if (qos != mqttWillConfigDTO.getQos()) {
        qos = mqttWillConfigDTO.getQos();
        hasChanged = true;
      }

      if (retain != mqttWillConfigDTO.isRetain()) {
        retain = mqttWillConfigDTO.isRetain();
        hasChanged = true;
      }

      if (delayInterval != mqttWillConfigDTO.getDelayInterval()) {
        delayInterval = mqttWillConfigDTO.getDelayInterval();
        hasChanged = true;
      }

      if (messageExpiryInterval != mqttWillConfigDTO.getMessageExpiryInterval()) {
        messageExpiryInterval = mqttWillConfigDTO.getMessageExpiryInterval();
        hasChanged = true;
      }

      if (!Objects.equals(contentType, mqttWillConfigDTO.getContentType())) {
        contentType = mqttWillConfigDTO.getContentType();
        hasChanged = true;
      }

      if (payloadFormatIndicator != mqttWillConfigDTO.getPayloadFormatIndicator()) {
        payloadFormatIndicator = mqttWillConfigDTO.getPayloadFormatIndicator();
        hasChanged = true;
      }
    }

    return hasChanged;
  }
}