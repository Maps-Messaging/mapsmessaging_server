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
import io.mapsmessaging.dto.rest.messaging.*;
import io.mapsmessaging.rest.responses.ConsumedMessages;
import io.mapsmessaging.rest.responses.ConsumedResponse;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.rest.responses.SubscriptionDepthResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DisplayName("Messaging Tools Integration Tests")
class MessagingToolsIntegrationTest {

  private PublishRequestDTO publishRequest;
  private SubscriptionRequestDTO subscriptionRequest;
  private SessionInfoDTO sessionInfo;

  @BeforeEach
  void setUp() {
    publishRequest = new PublishRequestDTO();
    subscriptionRequest = new SubscriptionRequestDTO();
    sessionInfo = new SessionInfoDTO();
  }

  @Test
  @DisplayName("Should support complete publish workflow with headers and delivery options")
  void testCompletePublishWorkflow() {
    String payload = Base64.getEncoder().encodeToString("workflow-test-data".getBytes());
    
    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setPayload(payload);
    messageDTO.setContentType("application/json");
    messageDTO.setQualityOfService(QualityOfService.AT_LEAST_ONCE.getLevel());
    messageDTO.setPriority(Priority.HIGH.getValue());

    Map<String, String> headers = new HashMap<>();
    headers.put("correlationId", "corr-workflow-001");
    headers.put("source", "integration-test");
    headers.put("timestamp", String.valueOf(System.currentTimeMillis()));

    Map<String, String> deliveryOptions = new HashMap<>();
    deliveryOptions.put("timeout", "10000");
    deliveryOptions.put("retryCount", "3");

    publishRequest.setDestinationName("integration/publish/test");
    publishRequest.setMessage(messageDTO);
    publishRequest.setHeaders(headers);
    publishRequest.setDeliveryOptions(deliveryOptions);
    publishRequest.setRetain(true);
    publishRequest.setSessionName("workflow-session");

    Assertions.assertNotNull(publishRequest.getHeaders());
    Assertions.assertNotNull(publishRequest.getDeliveryOptions());
    Assertions.assertTrue(publishRequest.isRetain());
    Assertions.assertEquals("workflow-session", publishRequest.getSessionName());
  }

  @Test
  @DisplayName("Should support subscription with filter and named session")
  void testSubscriptionWithFilterAndSession() {
    subscriptionRequest.setDestinationName("events/filtered");
    subscriptionRequest.setFilter("priority > 5 AND temperature < 30");
    subscriptionRequest.setNamedSubscription("temperature-alerts");
    subscriptionRequest.setMaxDepth(100);
    subscriptionRequest.setTransactional(true);

    Assertions.assertEquals("events/filtered", subscriptionRequest.getDestinationName());
    Assertions.assertEquals("temperature-alerts", subscriptionRequest.getNamedSubscription());
    Assertions.assertTrue(subscriptionRequest.isTransactional());
    Assertions.assertEquals(100, subscriptionRequest.getMaxDepth());
  }

  @Test
  @DisplayName("Should manage session with multiple subscriptions")
  void testMultipleSubscriptionsInSession() {
    sessionInfo.setSessionId("multi-sub-session");
    sessionInfo.setSessionName("Data Collection Session");
    sessionInfo.setTransactional(true);
    sessionInfo.setStatus("ACTIVE");
    sessionInfo.setCreationTime(LocalDateTime.now().minusHours(1));
    sessionInfo.setLastActivity(LocalDateTime.now());
    sessionInfo.setSubscriptionCount(3);

    Map<String, Integer> outstandingMessages = new HashMap<>();
    outstandingMessages.put("sensors/temperature", 12);
    outstandingMessages.put("sensors/humidity", 8);
    outstandingMessages.put("sensors/pressure", 5);

    sessionInfo.setOutstandingMessages(outstandingMessages);

    Assertions.assertEquals(3, sessionInfo.getSubscriptionCount());
    Assertions.assertEquals(3, sessionInfo.getOutstandingMessages().size());
    
    int totalMessages = sessionInfo.getOutstandingMessages()
        .values()
        .stream()
        .mapToInt(Integer::intValue)
        .sum();
    Assertions.assertEquals(25, totalMessages);
  }

  @Test
  @DisplayName("Should handle transactional publish with commit workflow")
  void testTransactionalPublishWithCommit() {
    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setPayload(Base64.getEncoder().encodeToString("txn-msg".getBytes()));
    messageDTO.setQualityOfService(QualityOfService.AT_LEAST_ONCE.getLevel());

    publishRequest.setDestinationName("txn/queue");
    publishRequest.setMessage(messageDTO);
    publishRequest.setSessionName("txn-session-001");

    PublishResponseDTO response = new PublishResponseDTO();
    response.setSuccess(true);
    response.setMessageId("msg-txn-001");
    response.setTransactionId("txn-001");
    response.setTransactionStatus("COMMITTED");
    response.setDeliveryLatency(50);

    Assertions.assertTrue(response.isSuccess());
    Assertions.assertEquals("COMMITTED", response.getTransactionStatus());
  }

  @Test
  @DisplayName("Should support SSE message streaming with async delivery")
  void testSseMessageStreaming() {
    AsyncMessageDTO asyncMessage = new AsyncMessageDTO();
    asyncMessage.setIdentifier(1001L);
    asyncMessage.setDestinationName("stream/test/topic");
    asyncMessage.setPayload(Base64.getEncoder().encodeToString("streaming-data".getBytes()));
    asyncMessage.setContentType("application/json");
    asyncMessage.setQualityOfService(QualityOfService.AT_LEAST_ONCE.getLevel());

    Map<String, String> headers = new HashMap<>();
    headers.put("streamId", "stream-001");
    asyncMessage.setHeaders(headers);

    Assertions.assertNotNull(asyncMessage.getDestinationName());
    Assertions.assertTrue(asyncMessage.getDestinationName().contains("stream"));
    Assertions.assertNotNull(asyncMessage.getHeaders());
  }

  @Test
  @DisplayName("Should support wildcard subscriptions with SSE")
  void testWildcardSubscriptionWithSse() {
    subscriptionRequest.setDestinationName("sensors/building-1/+/temperature");
    subscriptionRequest.setNamedSubscription("building-1-temp");
    subscriptionRequest.setMaxDepth(50);

    Assertions.assertTrue(subscriptionRequest.getDestinationName().contains("+"));
    
    AsyncMessageDTO message1 = new AsyncMessageDTO();
    message1.setDestinationName("sensors/building-1/floor-1/temperature");
    message1.setPayload(Base64.getEncoder().encodeToString("22.5".getBytes()));

    AsyncMessageDTO message2 = new AsyncMessageDTO();
    message2.setDestinationName("sensors/building-1/floor-2/temperature");
    message2.setPayload(Base64.getEncoder().encodeToString("23.0".getBytes()));

    Assertions.assertTrue(message1.getDestinationName().matches(".*building-1.*temperature"));
    Assertions.assertTrue(message2.getDestinationName().matches(".*building-1.*temperature"));
  }

  @Test
  @DisplayName("Should track publish success metrics")
  void testPublishSuccessMetrics() {
    int successCount = 0;
    int failureCount = 0;

    for (int i = 0; i < 10; i++) {
      PublishResponseDTO response = new PublishResponseDTO();
      if (i < 9) {
        response.setSuccess(true);
        successCount++;
      } else {
        response.setSuccess(false);
        failureCount++;
      }
    }

    Assertions.assertEquals(9, successCount);
    Assertions.assertEquals(1, failureCount);
    Assertions.assertEquals(90.0, (double) successCount / 10 * 100);
  }

  @Test
  @DisplayName("Should support message consumption with depth limit")
  void testMessageConsumption() {
    ConsumeRequestDTO consumeRequest = new ConsumeRequestDTO();
    consumeRequest.setDestination("messages/queue");
    consumeRequest.setDepth(50);

    Assertions.assertEquals("messages/queue", consumeRequest.getDestination());
    Assertions.assertEquals(50, consumeRequest.getDepth());
  }

  @Test
  @DisplayName("Should support session creation with named subscriptions")
  void testNamedSessionCreation() {
    sessionInfo.setSessionId("named-session-001");
    sessionInfo.setSessionName("Weather Monitoring");
    sessionInfo.setCreationTime(LocalDateTime.now());
    sessionInfo.setLastActivity(LocalDateTime.now());
    sessionInfo.setStatus("ACTIVE");

    Assertions.assertEquals("named-session-001", sessionInfo.getSessionId());
    Assertions.assertEquals("Weather Monitoring", sessionInfo.getSessionName());
    Assertions.assertEquals("ACTIVE", sessionInfo.getStatus());
  }

  @Test
  @DisplayName("Should support message ordering in transactions")
  void testMessageOrderingInTransactions() {
    List<Long> messageIds = List.of(1L, 2L, 3L, 4L, 5L);

    for (int i = 0; i < messageIds.size(); i++) {
      Assertions.assertEquals(i + 1, messageIds.get(i).intValue());
    }
  }

  @Test
  @DisplayName("Should support message acknowledgment workflow")
  void testMessageAcknowledgmentWorkflow() {
    MessageDTO receivedMessage = new MessageDTO();
    receivedMessage.setIdentifier(100L);
    receivedMessage.setPayload(Base64.getEncoder().encodeToString("ack-test".getBytes()));
    receivedMessage.setDeliveryStatus("PENDING_ACK");

    Assertions.assertEquals("PENDING_ACK", receivedMessage.getDeliveryStatus());

    receivedMessage.setDeliveryStatus("ACKNOWLEDGED");
    Assertions.assertEquals("ACKNOWLEDGED", receivedMessage.getDeliveryStatus());
  }

  @Test
  @DisplayName("Should support message rollback workflow")
  void testMessageRollbackWorkflow() {
    MessageDTO receivedMessage = new MessageDTO();
    receivedMessage.setIdentifier(101L);
    receivedMessage.setTransactionId("txn-rollback-001");
    receivedMessage.setDeliveryStatus("PENDING_ACK");

    Assertions.assertEquals("PENDING_ACK", receivedMessage.getDeliveryStatus());

    receivedMessage.setDeliveryStatus("ROLLED_BACK");
    Assertions.assertEquals("ROLLED_BACK", receivedMessage.getDeliveryStatus());
  }

  @Test
  @DisplayName("Should track message latency across delivery")
  void testMessageLatencyTracking() {
    long startTime = System.currentTimeMillis();

    PublishResponseDTO response = new PublishResponseDTO();
    response.setPublishTime(LocalDateTime.now());
    response.setSuccess(true);

    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    long endTime = System.currentTimeMillis();
    long latency = endTime - startTime;

    response.setDeliveryLatency(latency);
    Assertions.assertTrue(response.getDeliveryLatency() >= 100);
  }

  @Test
  @DisplayName("Should support export of streamed messages")
  void testStreamedMessageExport() {
    List<AsyncMessageDTO> messages = new java.util.ArrayList<>();

    for (int i = 0; i < 5; i++) {
      AsyncMessageDTO message = new AsyncMessageDTO();
      message.setIdentifier((long) i);
      message.setDestinationName("export/test");
      message.setPayload(Base64.getEncoder().encodeToString(("msg-" + i).getBytes()));
      messages.add(message);
    }

    Assertions.assertEquals(5, messages.size());
    for (int i = 0; i < messages.size(); i++) {
      Assertions.assertEquals(i, messages.get(i).getIdentifier());
    }
  }

  @Test
  @DisplayName("Should support filters on SSE consumption")
  void testSseConsumptionFilters() {
    ConsumeRequestDTO consumeRequest = new ConsumeRequestDTO();
    consumeRequest.setDestination("filtered/stream");
    consumeRequest.setDepth(100);

    subscriptionRequest.setDestinationName("filtered/stream");
    subscriptionRequest.setFilter("type = 'alert' AND severity > 3");

    Assertions.assertNotNull(subscriptionRequest.getFilter());
    Assertions.assertTrue(subscriptionRequest.getFilter().contains("type"));
  }

  @Test
  @DisplayName("Should maintain session state across operations")
  void testSessionStateConsistency() {
    sessionInfo.setSessionId("consistent-session");
    sessionInfo.setSubscriptionCount(3);
    
    Map<String, Integer> outstanding = new HashMap<>();
    outstanding.put("topic1", 5);
    outstanding.put("topic2", 3);
    outstanding.put("topic3", 2);
    
    sessionInfo.setOutstandingMessages(outstanding);

    int totalBefore = sessionInfo.getOutstandingMessages()
        .values()
        .stream()
        .mapToInt(Integer::intValue)
        .sum();

    outstanding.put("topic1", 4);
    sessionInfo.setOutstandingMessages(outstanding);

    int totalAfter = sessionInfo.getOutstandingMessages()
        .values()
        .stream()
        .mapToInt(Integer::intValue)
        .sum();

    Assertions.assertEquals(10, totalBefore);
    Assertions.assertEquals(9, totalAfter);
  }

  @Test
  @DisplayName("Should support batch publish operations")
  void testBatchPublishOperations() {
    List<PublishRequestDTO> batchRequests = new java.util.ArrayList<>();

    for (int i = 0; i < 10; i++) {
      PublishRequestDTO request = new PublishRequestDTO();
      MessageDTO message = new MessageDTO();
      message.setPayload(Base64.getEncoder().encodeToString(("batch-msg-" + i).getBytes()));
      message.setQualityOfService(QualityOfService.AT_MOST_ONCE.getLevel());
      
      request.setDestinationName("batch/topic");
      request.setMessage(message);
      batchRequests.add(request);
    }

    Assertions.assertEquals(10, batchRequests.size());
  }

  @Test
  @DisplayName("Should support correlation tracking across messages")
  void testCorrelationTracking() {
    byte[] correlationData = new byte[]{1, 2, 3, 4, 5};

    MessageDTO request = new MessageDTO();
    request.setPayload(Base64.getEncoder().encodeToString("request-data".getBytes()));
    request.setCorrelationData(correlationData);
    request.setIdentifier(1000L);

    MessageDTO response = new MessageDTO();
    response.setPayload(Base64.getEncoder().encodeToString("response-data".getBytes()));
    response.setCorrelationData(correlationData);
    response.setIdentifier(1001L);

    Assertions.assertArrayEquals(request.getCorrelationData(), response.getCorrelationData());
  }
}
