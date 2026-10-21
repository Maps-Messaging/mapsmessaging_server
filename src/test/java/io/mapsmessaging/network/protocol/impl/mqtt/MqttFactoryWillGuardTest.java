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

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.dto.rest.config.network.EndPointConnectionServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.MqttWillConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Publish;
import io.mapsmessaging.network.protocol.impl.mqtt5.MQTT5Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt5.MQTT5ProtocolFactory;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Publish5;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MqttFactoryWillGuardTest {

  @ParameterizedTest
  @CsvSource({"false,0", "false,1", "false,2", "false,3",
      "true,0", "true,1", "true,2", "true,3"})
  void connection_type_and_optional_will_preserve_payload_and_topic_mapping(
      boolean mqtt5, int scenario) throws Exception {
    EndPointServerConfigDTO config = new EndPointServerConfigDTO();
    if (scenario > 0) {
      EndPointConnectionServerConfigDTO connection = new EndPointConnectionServerConfigDTO();
      if (scenario > 1) {
        MqttWillConfigDTO will = new MqttWillConfigDTO();
        will.setTopic("/will");
        will.setQos(1);
        will.setRetain(true);
        will.setPayloadEncoding(scenario == 3 ? "base64" : "utf8");
        will.setPayload(scenario == 3 ? "AQID" : "sensor-µ");
        connection.setWillConfig(will);
      }
      config = connection;
    }
    EndPoint endpoint = mock(EndPoint.class, RETURNS_DEEP_STUBS);
    when(endpoint.getServer().getConfig()).thenReturn(config);
    Map<String, String> mappings = new HashMap<>();
    byte[] expected = scenario == 3 ? new byte[]{1, 2, 3}
        : "sensor-µ".getBytes(StandardCharsets.UTF_8);

    if (mqtt5) {
      try (MockedConstruction<MQTT5Protocol> construction = mockConstruction(MQTT5Protocol.class,
          (protocol, context) -> when(protocol.getTopicNameMapping()).thenReturn(mappings))) {
        new MQTT5ProtocolFactory().connect(endpoint, "client", "user", "password",
            Map.of("remote", "local"));
        ArgumentCaptor<Publish5> captured = ArgumentCaptor.forClass(Publish5.class);
        verify(construction.constructed().getFirst()).connect(
            eq("client"), eq("user"), eq("password"), captured.capture());
        if (scenario < 2) {
          assertNull(captured.getValue());
        } else {
          assertArrayEquals(expected, captured.getValue().getPayload());
          assertEquals("/will", captured.getValue().getDestinationName());
          assertEquals(true, captured.getValue().isRetain());
        }
      }
    } else {
      try (MockedConstruction<MQTTProtocol> construction = mockConstruction(MQTTProtocol.class,
          (protocol, context) -> when(protocol.getTopicNameMapping()).thenReturn(mappings))) {
        new MQTTProtocolFactory().connect(endpoint, "client", "user", "password",
            Map.of("remote", "local"));
        ArgumentCaptor<Publish> captured = ArgumentCaptor.forClass(Publish.class);
        verify(construction.constructed().getFirst()).connect(
            eq("client"), eq("user"), eq("password"), captured.capture());
        if (scenario < 2) {
          assertNull(captured.getValue());
        } else {
          assertArrayEquals(expected, captured.getValue().getPayload());
          assertEquals("/will", captured.getValue().getDestinationName());
          assertEquals(true, captured.getValue().isRetain());
        }
      }
    }
    assertEquals(Map.of("remote", "local"), mappings);
  }
}
