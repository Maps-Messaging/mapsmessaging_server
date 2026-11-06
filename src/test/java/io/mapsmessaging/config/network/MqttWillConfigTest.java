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

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.network.MqttWillConfigDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MqttWillConfigTest {

  @Test
  void round_trip_preserves_all_fields() {
    MqttWillConfig original = new MqttWillConfig(properties());

    MqttWillConfig roundTripped = new MqttWillConfig(original.toConfigurationProperties());

    assertEquals("system/status", roundTripped.getTopic());
    assertEquals("offline", roundTripped.getPayload());
    assertEquals("base64", roundTripped.getPayloadEncoding());
    assertEquals(2, roundTripped.getQos());
    assertTrue(roundTripped.isRetain());
    assertEquals(15, roundTripped.getDelayInterval());
    assertEquals(300L, roundTripped.getMessageExpiryInterval());
    assertEquals("application/json", roundTripped.getContentType());
    assertEquals(1, roundTripped.getPayloadFormatIndicator());
  }

  @Test
  void update_changes_payload_encoding_without_copying_payload() {
    MqttWillConfig config = new MqttWillConfig();
    config.setPayload("payload-value");
    config.setPayloadEncoding("string");

    MqttWillConfigDTO update = new MqttWillConfigDTO();
    update.setPayload("payload-value");
    update.setPayloadEncoding("base64");

    assertTrue(config.update(update));
    assertEquals("payload-value", config.getPayload());
    assertEquals("base64", config.getPayloadEncoding());
  }

  @Test
  void update_with_identical_values_returns_false() {
    MqttWillConfig config = new MqttWillConfig(properties());
    MqttWillConfigDTO update = new MqttWillConfigDTO();
    update.setTopic(config.getTopic());
    update.setPayload(config.getPayload());
    update.setPayloadEncoding(config.getPayloadEncoding());
    update.setQos(config.getQos());
    update.setRetain(config.isRetain());
    update.setDelayInterval(config.getDelayInterval());
    update.setMessageExpiryInterval(config.getMessageExpiryInterval());
    update.setContentType(config.getContentType());
    update.setPayloadFormatIndicator(config.getPayloadFormatIndicator());

    assertFalse(config.update(update));
  }

  @Test
  void update_rejects_unrelated_dto() {
    MqttWillConfig config = new MqttWillConfig(properties());

    assertFalse(config.update(new BaseConfigDTO()));
  }

  private ConfigurationProperties properties() {
    ConfigurationProperties properties = new ConfigurationProperties();
    properties.put("topic", "system/status");
    properties.put("payload", "offline");
    properties.put("payloadEncoding", "base64");
    properties.put("qos", 2);
    properties.put("retain", true);
    properties.put("delayInterval", 15);
    properties.put("messageExpiryInterval", 300L);
    properties.put("contentType", "application/json");
    properties.put("payloadFormatIndicator", 1);
    return properties;
  }
}
