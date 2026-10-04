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
import io.mapsmessaging.api.message.Message;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class MessageBinaryTransformationTest {

  @Test
  void reportsTransformationMetadata() {
    MessageBinaryTransformation transformation = new MessageBinaryTransformation();

    assertEquals("Message-Raw", transformation.getName());
    assertEquals(3, transformation.getId());
    assertEquals("Transforms messages to an internal binary payload and vice versa", transformation.getDescription());
  }

  @Test
  void roundTripPreservesPayloadAndMessagePropertiesAndMergesMetadata() {
    byte[] payload = "binary-payload".getBytes(StandardCharsets.UTF_8);
    Message original = new MessageBuilder()
        .setMeta(new LinkedHashMap<>(Map.of(
            "protocol", "MQTT",
            "messageType", "PUBLISH")))
        .setOpaqueData(payload)
        .setContentType("application/octet-stream")
        .setResponseTopic("/reply")
        .setPriority(Priority.HIGHEST)
        .setRetain(true)
        .build();

    try (MockedStatic<MessageDaemon> daemonStatic = mockDaemon()) {
      MessageBinaryTransformation transformation = new MessageBinaryTransformation();
      Message wireMessage = transformation.outgoing(original, "/topic/test");
      MessageBuilder inbound = new MessageBuilder(wireMessage);
      inbound.setMeta(new LinkedHashMap<>(Map.of("local", "preserved")));

      transformation.incoming(inbound);

      Message restored = inbound.build();
      assertArrayEquals(payload, restored.getOpaqueData());
      assertEquals("MQTT", restored.getMeta().get("protocol"));
      assertEquals("PUBLISH", restored.getMeta().get("messageType"));
      assertEquals("preserved", restored.getMeta().get("local"));
      assertTrue(restored.getMeta().containsKey("route"));
      assertEquals("application/octet-stream", restored.getContentType());
      assertEquals("/reply", restored.getResponseTopic());
      assertEquals(Priority.HIGHEST, restored.getPriority());
      assertTrue(restored.isRetain());
    }
  }

  @Test
  void outgoingLeavesInternalDestinationMessageUntouched() {
    Message original = new MessageBuilder()
        .setOpaqueData("payload".getBytes(StandardCharsets.UTF_8))
        .build();

    Message result = new MessageBinaryTransformation().outgoing(original, "$SYS/internal");

    assertSame(original, result);
  }

  @Test
  void incomingIgnoresMissingEmptyAndUnknownPayloads() {
    MessageBinaryTransformation transformation = new MessageBinaryTransformation();

    MessageBuilder missing = new MessageBuilder();
    transformation.incoming(missing);
    assertNull(missing.build().getOpaqueData());

    MessageBuilder empty = new MessageBuilder().setOpaqueData(new byte[0]);
    transformation.incoming(empty);
    assertArrayEquals(new byte[0], empty.build().getOpaqueData());

    byte[] unknown = {(byte) 0x80, 0x00};
    MessageBuilder unknownBuilder = new MessageBuilder().setOpaqueData(unknown);
    transformation.incoming(unknownBuilder);
    assertArrayEquals(unknown, unknownBuilder.build().getOpaqueData());
  }

  @Test
  void incomingHandlesMalformedBinaryFrameWithoutEscapingException() {
    byte[] malformed = {(byte) 0x81, 0x01, 0x00, 0x00, 0x00, 0x08, 0x01};
    MessageBuilder builder = new MessageBuilder().setOpaqueData(malformed);

    assertDoesNotThrow(() -> new MessageBinaryTransformation().incoming(builder));
    assertArrayEquals(malformed, builder.build().getOpaqueData());
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
