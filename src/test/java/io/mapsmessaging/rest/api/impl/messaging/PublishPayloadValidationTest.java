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

import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.dto.rest.messaging.MessageDTO;
import io.mapsmessaging.dto.rest.messaging.PublishRequestDTO;
import io.mapsmessaging.dto.rest.messaging.PublishResponseDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@DisplayName("Publish Payload Validation Tests")
class PublishPayloadValidationTest {

  @Test
  @DisplayName("Should validate JSON payload encoding")
  void testJsonPayloadEncoding() {
    String jsonPayload = "{\"temperature\": 25.5, \"humidity\": 60}";
    String encodedPayload = Base64.getEncoder().encodeToString(jsonPayload.getBytes());

    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setPayload(encodedPayload);
    messageDTO.setContentType("application/json");
    messageDTO.setQualityOfService(QualityOfService.AT_LEAST_ONCE.getLevel());
    messageDTO.setPriority(Priority.NORMAL.getValue());

    PublishRequestDTO request = new PublishRequestDTO();
    request.setDestinationName("sensor/temperature");
    request.setMessage(messageDTO);

    Assertions.assertNotNull(request.getMessage().getPayload());
    Assertions.assertEquals("application/json", request.getMessage().getContentType());
    Assertions.assertEquals(QualityOfService.AT_LEAST_ONCE.getLevel(), 
        request.getMessage().getQualityOfService());
  }

  @Test
  @DisplayName("Should validate text payload encoding")
  void testTextPayloadEncoding() {
    String textPayload = "This is plain text data";
    String encodedPayload = Base64.getEncoder().encodeToString(textPayload.getBytes());

    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setPayload(encodedPayload);
    messageDTO.setContentType("text/plain");
    messageDTO.setQualityOfService(QualityOfService.AT_MOST_ONCE.getLevel());

    PublishRequestDTO request = new PublishRequestDTO();
    request.setDestinationName("messages/text");
    request.setMessage(messageDTO);

    byte[] decodedPayload = Base64.getDecoder().decode(request.getMessage().getPayload());
    String decodedText = new String(decodedPayload);

    Assertions.assertEquals(textPayload, decodedText);
    Assertions.assertEquals("text/plain", request.getMessage().getContentType());
  }

  @Test
  @DisplayName("Should validate binary payload encoding")
  void testBinaryPayloadEncoding() {
    byte[] binaryData = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
    String encodedPayload = Base64.getEncoder().encodeToString(binaryData);

    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setPayload(encodedPayload);
    messageDTO.setContentType("application/octet-stream");
    messageDTO.setQualityOfService(QualityOfService.AT_LEAST_ONCE.getLevel());

    PublishRequestDTO request = new PublishRequestDTO();
    request.setDestinationName("binary/data");
    request.setMessage(messageDTO);

    byte[] decodedPayload = Base64.getDecoder().decode(request.getMessage().getPayload());
    Assertions.assertArrayEquals(binaryData, decodedPayload);
    Assertions.assertEquals("application/octet-stream", request.getMessage().getContentType());
  }

  @Test
  @DisplayName("Should validate message with headers")
  void testMessageWithHeaders() {
    String payload = Base64.getEncoder().encodeToString("test data".getBytes());
    
    Map<String, String> headers = new HashMap<>();
    headers.put("correlationId", "corr-123");
    headers.put("source", "sensor-1");
    headers.put("priority", "high");

    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setPayload(payload);
    messageDTO.setHeaders(headers);
    messageDTO.setContentType("application/json");

    PublishRequestDTO request = new PublishRequestDTO();
    request.setDestinationName("data/stream");
    request.setMessage(messageDTO);
    request.setHeaders(headers);

    Assertions.assertNotNull(request.getMessage().getHeaders());
    Assertions.assertEquals(3, request.getMessage().getHeaders().size());
    Assertions.assertEquals("corr-123", request.getMessage().getHeaders().get("correlationId"));
  }

  @Test
  @DisplayName("Should validate message with quality of service levels")
  void testQualityOfServiceValidation() {
    String payload = Base64.getEncoder().encodeToString("test".getBytes());

    for (QualityOfService qos : new QualityOfService[]{
        QualityOfService.AT_MOST_ONCE,
        QualityOfService.AT_LEAST_ONCE,
        QualityOfService.EXACTLY_ONCE
    }) {
      MessageDTO messageDTO = new MessageDTO();
      messageDTO.setPayload(payload);
      messageDTO.setQualityOfService(qos.getLevel());

      PublishRequestDTO request = new PublishRequestDTO();
      request.setDestinationName("qos/test");
      request.setMessage(messageDTO);

      Assertions.assertEquals(qos.getLevel(), request.getMessage().getQualityOfService());
    }
  }

  @Test
  @DisplayName("Should validate message with priority levels")
  void testPriorityValidation() {
    String payload = Base64.getEncoder().encodeToString("test".getBytes());

    int[] priorityLevels = {0, 4, 10};
    for (int priority : priorityLevels) {
      MessageDTO messageDTO = new MessageDTO();
      messageDTO.setPayload(payload);
      messageDTO.setPriority(priority);

      PublishRequestDTO request = new PublishRequestDTO();
      request.setDestinationName("priority/test");
      request.setMessage(messageDTO);

      Assertions.assertEquals(priority, request.getMessage().getPriority());
    }
  }

  @Test
  @DisplayName("Should validate message retention flag")
  void testMessageRetention() {
    String payload = Base64.getEncoder().encodeToString("persistent data".getBytes());

    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setPayload(payload);

    PublishRequestDTO request = new PublishRequestDTO();
    request.setDestinationName("retained/topic");
    request.setMessage(messageDTO);
    request.setRetain(true);

    Assertions.assertTrue(request.isRetain());
  }

  @Test
  @DisplayName("Should validate message with expiry time")
  void testMessageExpiryTime() {
    String payload = Base64.getEncoder().encodeToString("expiring data".getBytes());
    long expiryTime = 60000; // 60 seconds

    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setPayload(payload);
    messageDTO.setExpiry(expiryTime);

    PublishRequestDTO request = new PublishRequestDTO();
    request.setDestinationName("expiry/test");
    request.setMessage(messageDTO);

    Assertions.assertEquals(expiryTime, request.getMessage().getExpiry());
  }

  @Test
  @DisplayName("Should validate message with data map properties")
  void testMessageDataMapValidation() {
    String payload = Base64.getEncoder().encodeToString("test".getBytes());
    
    Map<String, Object> dataMap = new HashMap<>();
    dataMap.put("temperature", 25.5);
    dataMap.put("humidity", 60);
    dataMap.put("location", "room-1");

    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setPayload(payload);
    messageDTO.setDataMap(dataMap);

    PublishRequestDTO request = new PublishRequestDTO();
    request.setDestinationName("sensor/data");
    request.setMessage(messageDTO);

    Assertions.assertNotNull(request.getMessage().getDataMap());
    Assertions.assertEquals(3, request.getMessage().getDataMap().size());
    Assertions.assertEquals(25.5, request.getMessage().getDataMap().get("temperature"));
  }

  @Test
  @DisplayName("Should validate message with delivery options")
  void testDeliveryOptionsValidation() {
    String payload = Base64.getEncoder().encodeToString("data".getBytes());
    
    Map<String, String> deliveryOptions = new HashMap<>();
    deliveryOptions.put("timeout", "5000");
    deliveryOptions.put("retryCount", "3");
    deliveryOptions.put("backoffMs", "1000");

    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setPayload(payload);

    PublishRequestDTO request = new PublishRequestDTO();
    request.setDestinationName("delivery/test");
    request.setMessage(messageDTO);
    request.setDeliveryOptions(deliveryOptions);

    Assertions.assertNotNull(request.getDeliveryOptions());
    Assertions.assertEquals("5000", request.getDeliveryOptions().get("timeout"));
    Assertions.assertEquals("3", request.getDeliveryOptions().get("retryCount"));
  }

  @Test
  @DisplayName("Should validate publish request with transactional session")
  void testTransactionalPublishRequest() {
    String payload = Base64.getEncoder().encodeToString("txn data".getBytes());

    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setPayload(payload);
    messageDTO.setQualityOfService(QualityOfService.AT_LEAST_ONCE.getLevel());

    PublishRequestDTO request = new PublishRequestDTO();
    request.setDestinationName("txn/topic");
    request.setMessage(messageDTO);
    request.setSessionName("session-txn-1");

    Assertions.assertNotNull(request.getSessionName());
    Assertions.assertEquals("session-txn-1", request.getSessionName());
  }

  @Test
  @DisplayName("Should validate large payload encoding")
  void testLargePayloadEncoding() {
    StringBuilder largePayload = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      largePayload.append("This is line ").append(i).append(" with some data\n");
    }

    String encodedPayload = Base64.getEncoder().encodeToString(largePayload.toString().getBytes());
    byte[] decodedPayload = Base64.getDecoder().decode(encodedPayload);

    Assertions.assertEquals(largePayload.toString(), new String(decodedPayload));
  }

  @Test
  @DisplayName("Should validate empty payload")
  void testEmptyPayloadValidation() {
    String emptyPayload = Base64.getEncoder().encodeToString("".getBytes());

    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setPayload(emptyPayload);

    PublishRequestDTO request = new PublishRequestDTO();
    request.setDestinationName("empty/test");
    request.setMessage(messageDTO);

    byte[] decoded = Base64.getDecoder().decode(request.getMessage().getPayload());
    Assertions.assertEquals(0, decoded.length);
  }

  @Test
  @DisplayName("Should validate special characters in payload")
  void testSpecialCharactersPayload() {
    String specialPayload = "Special chars: !@#$%^&*()_+-=[]{}|;:',.<>?/~`";
    String encodedPayload = Base64.getEncoder().encodeToString(specialPayload.getBytes());

    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setPayload(encodedPayload);

    PublishRequestDTO request = new PublishRequestDTO();
    request.setDestinationName("special/chars");
    request.setMessage(messageDTO);

    byte[] decodedPayload = Base64.getDecoder().decode(request.getMessage().getPayload());
    Assertions.assertEquals(specialPayload, new String(decodedPayload));
  }

  @Test
  @DisplayName("Should validate unicode characters in payload")
  void testUnicodePayload() {
    String unicodePayload = "Unicode: 你好世界 🌍 Привет мир";
    String encodedPayload = Base64.getEncoder().encodeToString(unicodePayload.getBytes());

    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setPayload(encodedPayload);
    messageDTO.setContentType("text/plain;charset=utf-8");

    PublishRequestDTO request = new PublishRequestDTO();
    request.setDestinationName("unicode/test");
    request.setMessage(messageDTO);

    byte[] decodedPayload = Base64.getDecoder().decode(request.getMessage().getPayload());
    String decodedText = new String(decodedPayload);
    Assertions.assertEquals(unicodePayload, decodedText);
  }

  @Test
  @DisplayName("Should validate correlation data")
  void testCorrelationDataValidation() {
    String payload = Base64.getEncoder().encodeToString("correlated data".getBytes());
    byte[] correlationData = new byte[]{1, 2, 3, 4, 5};

    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setPayload(payload);
    messageDTO.setCorrelationData(correlationData);

    PublishRequestDTO request = new PublishRequestDTO();
    request.setDestinationName("correlation/test");
    request.setMessage(messageDTO);

    Assertions.assertNotNull(request.getMessage().getCorrelationData());
    Assertions.assertArrayEquals(correlationData, request.getMessage().getCorrelationData());
  }
}
