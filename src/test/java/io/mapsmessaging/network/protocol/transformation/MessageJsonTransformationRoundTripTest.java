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

package io.mapsmessaging.network.protocol.transformation;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class MessageJsonTransformationRoundTripTest {

  @Test
  void stanag_like_message_round_trip_preserves_event_semantics() {
    byte[] payload = (
        "{\"body\":{\"action\":\"TaskAdminActionEnum_PUSH\","
            + "\"description\":{\"$discriminator\":\"TaskTypeEnum_PATROL\"}}}")
        .getBytes(StandardCharsets.UTF_8);

    try (MockedStatic<MessageDaemon> daemonStatic = mockDaemon()) {
      Message original = new MessageBuilder()
          .setMeta(new LinkedHashMap<>(Map.of(
              "protocol", "STANAG-4817",
              "messageType", "TASK_ADMIN")))
          .setOpaqueData(payload)
          .setContentType("application/json")
          .setResponseTopic("/4817/catl/test/replies")
          .setCorrelationData("TASK#1234")
          .setPriority(Priority.HIGHEST)
          .setQoS(QualityOfService.AT_LEAST_ONCE)
          .storeOffline(true)
          .setCreation(1_750_000_000_000L)
          .setPayloadUTF8(true)
          .build();

      Message restored = roundTrip(original);

      assertArrayEquals(payload, restored.getOpaqueData());
      assertEquals("STANAG-4817", restored.getMeta().get("protocol"));
      assertEquals("TASK_ADMIN", restored.getMeta().get("messageType"));
      assertTrue(restored.getMeta().get("route").contains("\"server\": \"test-server\""));
      assertEquals("application/json", restored.getContentType());
      assertEquals("/4817/catl/test/replies", restored.getResponseTopic());
      assertArrayEquals("TASK#1234".getBytes(StandardCharsets.UTF_8), restored.getCorrelationData());
      assertFalse(restored.isCorrelationDataByteArray());
      assertEquals(Priority.HIGHEST, restored.getPriority());
      assertEquals(QualityOfService.AT_LEAST_ONCE, restored.getQualityOfService());
      assertTrue(restored.isStoreOffline());
      assertTrue(restored.isUTF8());
      assertEquals(1_750_000_000_000L, restored.getCreation());
    }
  }

  @Test
  void mqtt5_binary_correlation_round_trip_preserves_original_bytes() {
    byte[] correlation = {0x00, 0x01, 0x02, 0x7f, (byte) 0x80, (byte) 0xff};

    try (MockedStatic<MessageDaemon> daemonStatic = mockDaemon()) {
      Message original = new MessageBuilder()
          .setOpaqueData("payload".getBytes(StandardCharsets.UTF_8))
          .setCorrelationData(correlation)
          .setQoS(QualityOfService.AT_LEAST_ONCE)
          .build();

      Message restored = roundTrip(original);

      assertArrayEquals(correlation, restored.getCorrelationData());
      assertTrue(restored.isCorrelationDataByteArray());
    }
  }

  private Message roundTrip(Message original) {
    MessageJsonTransformation transformation = new MessageJsonTransformation();
    Message wireMessage = transformation.outgoing(original, "/4817/catl/test");
    MessageBuilder inbound = new MessageBuilder(wireMessage);

    transformation.incoming(inbound);

    return inbound.build();
  }

  private MockedStatic<MessageDaemon> mockDaemon() {
    MessageDaemon daemon = mock(MessageDaemon.class);
    when(daemon.getHostname()).thenReturn("test-host");
    when(daemon.getId()).thenReturn("test-server");
    when(daemon.isTagMetaData()).thenReturn(false);

    MockedStatic<MessageDaemon> daemonStatic = mockStatic(MessageDaemon.class);
    daemonStatic.when(MessageDaemon::getInstance).thenReturn(daemon);
    return daemonStatic;
  }
}
