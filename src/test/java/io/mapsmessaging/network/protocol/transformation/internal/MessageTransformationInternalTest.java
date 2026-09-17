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

package io.mapsmessaging.network.protocol.transformation.internal;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageTransformationInternalTest {

  @Test
  void loader_restores_message_builder_state() {
    MessageLoader loader = new MessageLoader();
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("source", "remote");
    Map<String, TypedData> dataMap = new LinkedHashMap<>();
    dataMap.put("temperature", new TypedData(21.5));
    byte[] opaqueData = {1, 2, 3};

    loader.setMeta(meta);
    loader.setDataMap(dataMap);
    loader.setOpaqueData(opaqueData);
    loader.setContentType("application/octet-stream");
    loader.setResponseTopic("responses/device");
    loader.setPriority(Priority.HIGHEST);
    loader.setQualityOfService(QualityOfService.EXACTLY_ONCE);
    loader.setRetain(true);
    loader.setStoreOffline(true);
    loader.setUtf8(true);
    loader.setCreation(123_456L);
    loader.setSchemaId("schema-1");
    loader.setCorrelationData("correlation-id");

    MessageBuilder builder = new MessageBuilder();
    loader.load(builder);

    assertEquals(meta, builder.getMeta());
    assertEquals(dataMap, builder.getDataMap());
    assertArrayEquals(opaqueData, builder.getOpaqueData());
    assertEquals("application/octet-stream", builder.getContentType());
    assertEquals("responses/device", builder.getResponseTopic());
    assertEquals(Priority.HIGHEST, builder.getPriority());
    assertEquals(QualityOfService.EXACTLY_ONCE, builder.getQualityOfService());
    assertTrue(builder.isRetain());
    assertTrue(builder.isStoreOffline());
    assertTrue(builder.isPayloadUTF8());
    assertEquals(123_456L, builder.getCreation());
    assertEquals("schema-1", builder.getSchemaId());
    assertEquals("correlation-id", builder.getCorrelationData());
  }

  @Test
  void loader_merges_metadata_into_existing_builder_metadata() {
    MessageBuilder builder = new MessageBuilder();
    builder.setMeta(new LinkedHashMap<>(Map.of("local", "value")));
    MessageLoader loader = new MessageLoader();
    loader.setMeta(Map.of("remote", "value"));

    loader.load(builder);

    assertEquals("value", builder.getMeta().get("local"));
    assertEquals("value", builder.getMeta().get("remote"));
  }

  @Test
  void loader_converts_absolute_delay_and_expiry_to_builder_durations() {
    long now = System.currentTimeMillis();
    MessageLoader loader = new MessageLoader();
    loader.setDelayed(now + 60_000L);
    loader.setExpiry(now + 360_000L);

    MessageBuilder builder = new MessageBuilder();
    loader.load(builder);

    assertTrue(builder.getDelayed() > 55_000L && builder.getDelayed() <= 60_000L);
    assertTrue(builder.getExpiry() > 295_000L && builder.getExpiry() <= 300_000L);
  }

  @Test
  void packer_correlation_data_respects_binary_flag() {
    Message message = mock(Message.class);
    byte[] correlation = "correlation".getBytes(StandardCharsets.UTF_8);
    when(message.getCorrelationData()).thenReturn(correlation);
    MessagePacker packer = new MessagePacker(message);

    when(message.isCorrelationDataByteArray()).thenReturn(false);
    assertEquals("correlation", packer.getCorrelationData());

    when(message.isCorrelationDataByteArray()).thenReturn(true);
    assertSame(correlation, packer.getCorrelationData());

    when(message.getCorrelationData()).thenReturn(null);
    assertNull(packer.getCorrelationData());
  }

  @Test
  void packer_delegates_message_properties() {
    Message message = mock(Message.class);
    byte[] opaque = {9, 8, 7};
    Map<String, TypedData> dataMap = Map.of("value", new TypedData(42));
    when(message.getDataMap()).thenReturn(dataMap);
    when(message.getOpaqueData()).thenReturn(opaque);
    when(message.getContentType()).thenReturn("application/json");
    when(message.getSchemaId()).thenReturn("schema");
    when(message.getResponseTopic()).thenReturn("reply");
    when(message.getIdentifier()).thenReturn(99L);
    when(message.getExpiry()).thenReturn(100L);
    when(message.getDelayed()).thenReturn(200L);
    when(message.getCreation()).thenReturn(300L);
    when(message.getPriority()).thenReturn(Priority.ONE_ABOVE_NORMAL);
    when(message.getQualityOfService()).thenReturn(QualityOfService.AT_LEAST_ONCE);
    when(message.isRetain()).thenReturn(true);
    when(message.isStoreOffline()).thenReturn(true);
    when(message.isLastMessage()).thenReturn(true);
    when(message.isUTF8()).thenReturn(true);
    when(message.isCorrelationDataByteArray()).thenReturn(false);

    MessagePacker packer = new MessagePacker(message);

    assertEquals(dataMap, packer.getDataMap());
    assertArrayEquals(opaque, packer.getOpaqueData());
    assertEquals("application/json", packer.getContentType());
    assertEquals("schema", packer.getSchemaId());
    assertEquals("reply", packer.getResponseTopic());
    assertEquals(99L, packer.getIdentifier());
    assertEquals(100L, packer.getExpiry());
    assertEquals(200L, packer.getDelayed());
    assertEquals(300L, packer.getCreation());
    assertEquals(Priority.ONE_ABOVE_NORMAL, packer.getPriority());
    assertEquals(QualityOfService.AT_LEAST_ONCE, packer.getQualityOfService());
    assertTrue(packer.isRetain());
    assertTrue(packer.isStoreOffline());
    assertTrue(packer.isLastMessage());
    assertTrue(packer.isUTF8());
    assertFalse(packer.isCorrelationDataByteArray());
  }

  @Test
  void route_handler_adds_route_without_mutating_input_metadata() {
    Map<String, String> original = new LinkedHashMap<>();
    original.put("key", "value");

    Map<String, String> updated = MetaRouteHandler.updateRoute("remote-host", "server-1", original,
        System.currentTimeMillis() - 100L);

    assertEquals(Map.of("key", "value"), original);
    assertEquals("value", updated.get("key"));
    assertTrue(updated.get("route").contains("\"server\": \"server-1\""));
    assertTrue(updated.get("route").contains("\"host\": \"remote-host\""));
    assertTrue(updated.get("route").contains("\"hop\": 1"));
  }

  @Test
  void route_handler_increments_hop_for_existing_route() {
    Map<String, String> meta = Map.of(
        "route",
        "[{\"server\": \"server-1\", \"host\": \"host-1\", \"age\": 10, \"hop\": 1}]"
    );

    Map<String, String> updated = MetaRouteHandler.updateRoute("host-2", "server-2", meta,
        System.currentTimeMillis());

    assertTrue(updated.get("route").contains("\"server\": \"server-1\""));
    assertTrue(updated.get("route").contains("\"server\": \"server-2\""));
    assertTrue(updated.get("route").contains("\"hop\": 2"));
  }
}
