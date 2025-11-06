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

package io.mapsmessaging.rest.api.impl.messaging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.dto.rest.messaging.AsyncMessageDTO;
import io.mapsmessaging.dto.rest.messaging.MessageDTO;
import io.mapsmessaging.rest.translation.GsonDateTimeSerialiser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@DisplayName("SSE Stream Parsing Tests")
class SseStreamParsingTest {

  private Gson gson;

  @BeforeEach
  void setUp() {
    gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDateTime.class, new GsonDateTimeSerialiser())
        .create();
  }

  @Test
  @DisplayName("Should parse basic async message DTO from SSE")
  void testParseBasicAsyncMessage() {
    AsyncMessageDTO message = new AsyncMessageDTO();
    message.setIdentifier(123L);
    message.setDestinationName("sensor/temperature");
    message.setPayload(Base64.getEncoder().encodeToString("25.5".getBytes()));
    message.setContentType("text/plain");
    message.setQualityOfService(QualityOfService.AT_LEAST_ONCE.getLevel());
    message.setPriority(Priority.NORMAL.getValue());

    String json = gson.toJson(message);
    Assertions.assertNotNull(json);
    Assertions.assertTrue(json.contains("sensor/temperature"));
    Assertions.assertTrue(json.contains("\"identifier\":123"));

    AsyncMessageDTO parsed = gson.fromJson(json, AsyncMessageDTO.class);
    Assertions.assertEquals(message.getIdentifier(), parsed.getIdentifier());
    Assertions.assertEquals(message.getDestinationName(), parsed.getDestinationName());
    Assertions.assertEquals(message.getContentType(), parsed.getContentType());
  }

  @Test
  @DisplayName("Should parse async message with headers")
  void testParseAsyncMessageWithHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put("correlationId", "corr-456");
    headers.put("source", "sensor-1");

    AsyncMessageDTO message = new AsyncMessageDTO();
    message.setIdentifier(124L);
    message.setDestinationName("sensor/humidity");
    message.setPayload(Base64.getEncoder().encodeToString("60%".getBytes()));
    message.setHeaders(headers);
    message.setContentType("text/plain");

    String json = gson.toJson(message);
    AsyncMessageDTO parsed = gson.fromJson(json, AsyncMessageDTO.class);

    Assertions.assertNotNull(parsed.getHeaders());
    Assertions.assertEquals("corr-456", parsed.getHeaders().get("correlationId"));
    Assertions.assertEquals("sensor-1", parsed.getHeaders().get("source"));
  }

  @Test
  @DisplayName("Should parse async message with metadata")
  void testParseAsyncMessageWithMetadata() {
    Map<String, String> metadata = new LinkedHashMap<>();
    metadata.put("time_ms", String.valueOf(System.currentTimeMillis()));
    metadata.put("server", "server-1");
    metadata.put("protocol", "MQTT");

    AsyncMessageDTO message = new AsyncMessageDTO();
    message.setIdentifier(125L);
    message.setDestinationName("system/metrics");
    message.setPayload(Base64.getEncoder().encodeToString("{}".getBytes()));
    message.setMetaData(metadata);

    String json = gson.toJson(message);
    AsyncMessageDTO parsed = gson.fromJson(json, AsyncMessageDTO.class);

    Assertions.assertNotNull(parsed.getMetaData());
    Assertions.assertTrue(parsed.getMetaData().containsKey("time_ms"));
    Assertions.assertEquals("server-1", parsed.getMetaData().get("server"));
  }

  @Test
  @DisplayName("Should parse async message with data map")
  void testParseAsyncMessageWithDataMap() {
    Map<String, Object> dataMap = new LinkedHashMap<>();
    dataMap.put("temperature", 25.5);
    dataMap.put("humidity", 60);
    dataMap.put("pressure", 1013.25);

    AsyncMessageDTO message = new AsyncMessageDTO();
    message.setIdentifier(126L);
    message.setDestinationName("weather/station1");
    message.setPayload(Base64.getEncoder().encodeToString("{\"temp\": 25.5}".getBytes()));
    message.setDataMap(dataMap);

    String json = gson.toJson(message);
    AsyncMessageDTO parsed = gson.fromJson(json, AsyncMessageDTO.class);

    Assertions.assertNotNull(parsed.getDataMap());
    Assertions.assertEquals(25.5, parsed.getDataMap().get("temperature"));
    Assertions.assertEquals(60, parsed.getDataMap().get("humidity"));
  }

  @Test
  @DisplayName("Should parse async message with expiry")
  void testParseAsyncMessageWithExpiry() {
    long expiryTime = 60000; // 60 seconds

    AsyncMessageDTO message = new AsyncMessageDTO();
    message.setIdentifier(127L);
    message.setDestinationName("expiring/messages");
    message.setPayload(Base64.getEncoder().encodeToString("data".getBytes()));
    message.setExpiry(expiryTime);

    String json = gson.toJson(message);
    AsyncMessageDTO parsed = gson.fromJson(json, AsyncMessageDTO.class);

    Assertions.assertEquals(expiryTime, parsed.getExpiry());
  }

  @Test
  @DisplayName("Should parse async message with correlation data")
  void testParseAsyncMessageWithCorrelationData() {
    byte[] correlationData = new byte[]{10, 20, 30, 40};

    AsyncMessageDTO message = new AsyncMessageDTO();
    message.setIdentifier(128L);
    message.setDestinationName("requests/processing");
    message.setPayload(Base64.getEncoder().encodeToString("request".getBytes()));
    message.setCorrelationData(correlationData);

    String json = gson.toJson(message);
    AsyncMessageDTO parsed = gson.fromJson(json, AsyncMessageDTO.class);

    Assertions.assertNotNull(parsed.getCorrelationData());
    Assertions.assertArrayEquals(correlationData, parsed.getCorrelationData());
  }

  @Test
  @DisplayName("Should parse async message with creation timestamp")
  void testParseAsyncMessageWithCreationTime() {
    LocalDateTime now = LocalDateTime.now();

    AsyncMessageDTO message = new AsyncMessageDTO();
    message.setIdentifier(129L);
    message.setDestinationName("timestamp/test");
    message.setPayload(Base64.getEncoder().encodeToString("timestamped".getBytes()));
    message.setCreation(now);

    String json = gson.toJson(message);
    AsyncMessageDTO parsed = gson.fromJson(json, AsyncMessageDTO.class);

    Assertions.assertNotNull(parsed.getCreation());
  }

  @Test
  @DisplayName("Should parse async message with transaction ID")
  void testParseAsyncMessageWithTransactionId() {
    AsyncMessageDTO message = new AsyncMessageDTO();
    message.setIdentifier(130L);
    message.setDestinationName("transactions/inbox");
    message.setPayload(Base64.getEncoder().encodeToString("txn-data".getBytes()));
    message.setTransactionId("txn-9999");

    String json = gson.toJson(message);
    AsyncMessageDTO parsed = gson.fromJson(json, AsyncMessageDTO.class);

    Assertions.assertEquals("txn-9999", parsed.getTransactionId());
  }

  @Test
  @DisplayName("Should parse async message with delivery status")
  void testParseAsyncMessageWithDeliveryStatus() {
    AsyncMessageDTO message = new AsyncMessageDTO();
    message.setIdentifier(131L);
    message.setDestinationName("status/tracking");
    message.setPayload(Base64.getEncoder().encodeToString("status-msg".getBytes()));
    message.setDeliveryStatus("DELIVERED");

    String json = gson.toJson(message);
    AsyncMessageDTO parsed = gson.fromJson(json, AsyncMessageDTO.class);

    Assertions.assertEquals("DELIVERED", parsed.getDeliveryStatus());
  }

  @Test
  @DisplayName("Should parse async message with all QoS levels")
  void testParseAsyncMessageAllQosLevels() {
    for (QualityOfService qos : new QualityOfService[]{
        QualityOfService.AT_MOST_ONCE,
        QualityOfService.AT_LEAST_ONCE,
        QualityOfService.EXACTLY_ONCE
    }) {
      AsyncMessageDTO message = new AsyncMessageDTO();
      message.setIdentifier(132L);
      message.setDestinationName("qos/test/" + qos.name());
      message.setPayload(Base64.getEncoder().encodeToString("qos-data".getBytes()));
      message.setQualityOfService(qos.getLevel());

      String json = gson.toJson(message);
      AsyncMessageDTO parsed = gson.fromJson(json, AsyncMessageDTO.class);

      Assertions.assertEquals(qos.getLevel(), parsed.getQualityOfService());
    }
  }

  @Test
  @DisplayName("Should parse async message with all priority levels")
  void testParseAsyncMessageAllPriorityLevels() {
    int[] priorityLevels = {0, 5, 10};
    for (int priority : priorityLevels) {
      AsyncMessageDTO message = new AsyncMessageDTO();
      message.setIdentifier(133L);
      message.setDestinationName("priority/test/" + priority);
      message.setPayload(Base64.getEncoder().encodeToString("priority-data".getBytes()));
      message.setPriority(priority);

      String json = gson.toJson(message);
      AsyncMessageDTO parsed = gson.fromJson(json, AsyncMessageDTO.class);

      Assertions.assertEquals(priority, parsed.getPriority());
    }
  }

  @Test
  @DisplayName("Should parse json payload within async message")
  void testParseJsonPayloadInAsyncMessage() {
    String jsonContent = "{\"name\": \"sensor1\", \"reading\": 42.5}";
    String encodedPayload = Base64.getEncoder().encodeToString(jsonContent.getBytes());

    AsyncMessageDTO message = new AsyncMessageDTO();
    message.setIdentifier(134L);
    message.setDestinationName("json/payload");
    message.setPayload(encodedPayload);
    message.setContentType("application/json");

    String json = gson.toJson(message);
    AsyncMessageDTO parsed = gson.fromJson(json, AsyncMessageDTO.class);

    String decodedPayload = new String(Base64.getDecoder().decode(parsed.getPayload()));
    Assertions.assertEquals(jsonContent, decodedPayload);
  }

  @Test
  @DisplayName("Should parse multiple async messages in sequence")
  void testParseMultipleAsyncMessages() {
    for (int i = 0; i < 10; i++) {
      AsyncMessageDTO message = new AsyncMessageDTO();
      message.setIdentifier((long) i);
      message.setDestinationName("batch/test/msg-" + i);
      message.setPayload(Base64.getEncoder().encodeToString(("message-" + i).getBytes()));
      message.setContentType("text/plain");

      String json = gson.toJson(message);
      AsyncMessageDTO parsed = gson.fromJson(json, AsyncMessageDTO.class);

      Assertions.assertEquals((long) i, parsed.getIdentifier());
      Assertions.assertEquals("batch/test/msg-" + i, parsed.getDestinationName());
    }
  }

  @Test
  @DisplayName("Should handle null fields in async message")
  void testParseAsyncMessageWithNullFields() {
    AsyncMessageDTO message = new AsyncMessageDTO();
    message.setIdentifier(135L);
    message.setDestinationName("sparse/message");
    message.setPayload(Base64.getEncoder().encodeToString("data".getBytes()));

    String json = gson.toJson(message);
    AsyncMessageDTO parsed = gson.fromJson(json, AsyncMessageDTO.class);

    Assertions.assertNull(parsed.getHeaders());
    Assertions.assertNull(parsed.getMetaData());
    Assertions.assertNull(parsed.getDataMap());
  }

  @Test
  @DisplayName("Should parse async message with complex destination path")
  void testParseAsyncMessageComplexDestinationPath() {
    AsyncMessageDTO message = new AsyncMessageDTO();
    message.setIdentifier(136L);
    message.setDestinationName("/building/floor-3/room-301/sensor/temperature");
    message.setPayload(Base64.getEncoder().encodeToString("22.5".getBytes()));

    String json = gson.toJson(message);
    AsyncMessageDTO parsed = gson.fromJson(json, AsyncMessageDTO.class);

    Assertions.assertEquals("/building/floor-3/room-301/sensor/temperature", 
        parsed.getDestinationName());
  }

  @Test
  @DisplayName("Should parse async message with wildcard patterns")
  void testParseAsyncMessageWildcardDestination() {
    AsyncMessageDTO message = new AsyncMessageDTO();
    message.setIdentifier(137L);
    message.setDestinationName("sensor/+/temperature/#");
    message.setPayload(Base64.getEncoder().encodeToString("data".getBytes()));

    String json = gson.toJson(message);
    AsyncMessageDTO parsed = gson.fromJson(json, AsyncMessageDTO.class);

    Assertions.assertTrue(parsed.getDestinationName().contains("+"));
    Assertions.assertTrue(parsed.getDestinationName().contains("#"));
  }

  @Test
  @DisplayName("Should handle large message payloads in SSE")
  void testParseAsyncMessageLargePayload() {
    StringBuilder largeContent = new StringBuilder();
    for (int i = 0; i < 100; i++) {
      largeContent.append("This is a large message content line ").append(i).append("\n");
    }

    String encodedPayload = Base64.getEncoder().encodeToString(largeContent.toString().getBytes());

    AsyncMessageDTO message = new AsyncMessageDTO();
    message.setIdentifier(138L);
    message.setDestinationName("large/payload");
    message.setPayload(encodedPayload);

    String json = gson.toJson(message);
    AsyncMessageDTO parsed = gson.fromJson(json, AsyncMessageDTO.class);

    String decodedPayload = new String(Base64.getDecoder().decode(parsed.getPayload()));
    Assertions.assertEquals(largeContent.toString(), decodedPayload);
  }
}
