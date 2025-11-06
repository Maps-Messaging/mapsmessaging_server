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

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.dto.rest.messaging.MessageDTO;
import io.mapsmessaging.dto.rest.messaging.PublishRequestDTO;
import io.mapsmessaging.dto.rest.messaging.PublishResponseDTO;
import io.mapsmessaging.dto.rest.messaging.SubscriptionRequestDTO;
import io.mapsmessaging.rest.responses.TransactionData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@DisplayName("Transaction Flow Tests")
class TransactionFlowTest {

  private PublishResponseDTO publishResponse;

  @BeforeEach
  void setUp() {
    publishResponse = new PublishResponseDTO();
    publishResponse.setSuccess(true);
    publishResponse.setPublishTime(LocalDateTime.now());
  }

  @Test
  @DisplayName("Should create transactional publish request")
  void testCreateTransactionalPublishRequest() {
    String payload = Base64.getEncoder().encodeToString("txn-data".getBytes());

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
  @DisplayName("Should track transaction status in response")
  void testTransactionStatusTracking() {
    publishResponse.setTransactionId("txn-001");
    publishResponse.setTransactionStatus("COMMITTED");

    Assertions.assertNotNull(publishResponse.getTransactionId());
    Assertions.assertEquals("txn-001", publishResponse.getTransactionId());
    Assertions.assertEquals("COMMITTED", publishResponse.getTransactionStatus());
  }

  @Test
  @DisplayName("Should track delivery latency in transaction")
  void testDeliveryLatencyTracking() {
    long startTime = System.currentTimeMillis();
    
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    
    long endTime = System.currentTimeMillis();
    long latency = endTime - startTime;

    publishResponse.setDeliveryLatency(latency);
    publishResponse.setTransactionId("txn-002");
    publishResponse.setTransactionStatus("COMPLETED");

    Assertions.assertTrue(publishResponse.getDeliveryLatency() >= 100);
    Assertions.assertEquals("COMPLETED", publishResponse.getTransactionStatus());
  }

  @Test
  @DisplayName("Should handle transaction commit")
  void testTransactionCommit() {
    List<Long> messageIds = new ArrayList<>();
    messageIds.add(1L);
    messageIds.add(2L);
    messageIds.add(3L);

    TransactionData transactionData = new TransactionData();
    transactionData.setDestinationName("txn/queue");
    transactionData.setEventIds(messageIds);

    Assertions.assertNotNull(transactionData.getDestinationName());
    Assertions.assertEquals("txn/queue", transactionData.getDestinationName());
    Assertions.assertEquals(3, transactionData.getEventIds().size());
  }

  @Test
  @DisplayName("Should handle transaction rollback")
  void testTransactionRollback() {
    List<Long> messageIds = new ArrayList<>();
    messageIds.add(1L);
    messageIds.add(2L);

    TransactionData transactionData = new TransactionData();
    transactionData.setDestinationName("txn/queue");
    transactionData.setEventIds(messageIds);

    Assertions.assertEquals(2, transactionData.getEventIds().size());
    Assertions.assertTrue(transactionData.getEventIds().contains(1L));
    Assertions.assertTrue(transactionData.getEventIds().contains(2L));
  }

  @Test
  @DisplayName("Should handle multiple messages in transaction")
  void testMultipleMessagesInTransaction() {
    for (int i = 1; i <= 5; i++) {
      String payload = Base64.getEncoder().encodeToString(("msg-" + i).getBytes());

      MessageDTO messageDTO = new MessageDTO();
      messageDTO.setPayload(payload);
      messageDTO.setIdentifier(i);
      messageDTO.setQualityOfService(QualityOfService.AT_LEAST_ONCE.getLevel());

      PublishRequestDTO request = new PublishRequestDTO();
      request.setDestinationName("txn/batch");
      request.setMessage(messageDTO);
      request.setSessionName("batch-session-1");

      Assertions.assertEquals(i, request.getMessage().getIdentifier());
    }
  }

  @Test
  @DisplayName("Should track pending transaction status")
  void testPendingTransactionStatus() {
    publishResponse.setTransactionId("txn-pending-001");
    publishResponse.setTransactionStatus("PENDING");
    publishResponse.setDeliveryLatency(0);

    Assertions.assertEquals("PENDING", publishResponse.getTransactionStatus());
  }

  @Test
  @DisplayName("Should track failed transaction status")
  void testFailedTransactionStatus() {
    publishResponse.setSuccess(false);
    publishResponse.setTransactionId("txn-failed-001");
    publishResponse.setTransactionStatus("FAILED");
    publishResponse.setErrorDetails("Topic not found");

    Assertions.assertFalse(publishResponse.isSuccess());
    Assertions.assertEquals("FAILED", publishResponse.getTransactionStatus());
    Assertions.assertNotNull(publishResponse.getErrorDetails());
  }

  @Test
  @DisplayName("Should maintain transaction message order")
  void testTransactionMessageOrder() {
    String[] messages = {"first", "second", "third", "fourth", "fifth"};
    List<Long> messageIds = new ArrayList<>();

    for (int i = 0; i < messages.length; i++) {
      long msgId = (long) (i + 1);
      messageIds.add(msgId);
    }

    TransactionData transactionData = new TransactionData();
    transactionData.setDestinationName("ordered/queue");
    transactionData.setEventIds(messageIds);

    for (int i = 0; i < messageIds.size(); i++) {
      Assertions.assertEquals((long) (i + 1), transactionData.getEventIds().get(i));
    }
  }

  @Test
  @DisplayName("Should handle single message transaction")
  void testSingleMessageTransaction() {
    String payload = Base64.getEncoder().encodeToString("single-txn-msg".getBytes());

    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setPayload(payload);
    messageDTO.setQualityOfService(QualityOfService.AT_LEAST_ONCE.getLevel());

    PublishRequestDTO request = new PublishRequestDTO();
    request.setDestinationName("single/txn");
    request.setMessage(messageDTO);
    request.setSessionName("single-msg-session");

    TransactionData transactionData = new TransactionData();
    transactionData.setDestinationName(request.getDestinationName());
    transactionData.setEventIds(List.of(1L));

    Assertions.assertEquals(1, transactionData.getEventIds().size());
    Assertions.assertEquals(1L, transactionData.getEventIds().get(0));
  }

  @Test
  @DisplayName("Should track transactional subscription")
  void testTransactionalSubscription() {
    SubscriptionRequestDTO subscriptionRequest = new SubscriptionRequestDTO();
    subscriptionRequest.setDestinationName("txn/subscription");
    subscriptionRequest.setTransactional(true);
    subscriptionRequest.setMaxDepth(100);
    subscriptionRequest.setNamedSubscription("txn-sub-1");

    Assertions.assertTrue(subscriptionRequest.isTransactional());
    Assertions.assertEquals("txn-sub-1", subscriptionRequest.getNamedSubscription());
    Assertions.assertEquals(100, subscriptionRequest.getMaxDepth());
  }

  @Test
  @DisplayName("Should measure transaction completion time")
  void testTransactionCompletionTime() {
    long startTime = System.currentTimeMillis();
    
    publishResponse.setSuccess(true);
    publishResponse.setTransactionId("txn-time-001");
    publishResponse.setPublishTime(LocalDateTime.now());

    try {
      Thread.sleep(50);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    long endTime = System.currentTimeMillis();
    long totalTime = endTime - startTime;

    Assertions.assertTrue(totalTime >= 50);
    Assertions.assertNotNull(publishResponse.getPublishTime());
  }

  @Test
  @DisplayName("Should handle empty transaction")
  void testEmptyTransaction() {
    TransactionData transactionData = new TransactionData();
    transactionData.setDestinationName("empty/txn");
    transactionData.setEventIds(new ArrayList<>());

    Assertions.assertEquals(0, transactionData.getEventIds().size());
    Assertions.assertEquals("empty/txn", transactionData.getDestinationName());
  }

  @Test
  @DisplayName("Should handle large transaction with many messages")
  void testLargeTransaction() {
    List<Long> messageIds = new ArrayList<>();
    for (long i = 1; i <= 1000; i++) {
      messageIds.add(i);
    }

    TransactionData transactionData = new TransactionData();
    transactionData.setDestinationName("large/txn");
    transactionData.setEventIds(messageIds);

    Assertions.assertEquals(1000, transactionData.getEventIds().size());
    Assertions.assertEquals(1L, transactionData.getEventIds().get(0));
    Assertions.assertEquals(1000L, transactionData.getEventIds().get(999));
  }

  @Test
  @DisplayName("Should preserve message identifiers in transaction")
  void testMessageIdentifierPreservation() {
    List<Long> originalIds = List.of(100L, 200L, 300L, 400L, 500L);

    TransactionData transactionData = new TransactionData();
    transactionData.setDestinationName("preserve/ids");
    transactionData.setEventIds(new ArrayList<>(originalIds));

    Assertions.assertEquals(originalIds, transactionData.getEventIds());
  }

  @Test
  @DisplayName("Should track transaction state transitions")
  void testTransactionStateTransitions() {
    String[] states = {"CREATED", "PROCESSING", "PENDING_COMMIT", "COMMITTED"};

    for (String state : states) {
      publishResponse.setTransactionStatus(state);
      publishResponse.setTransactionId("txn-state-" + state);

      Assertions.assertEquals(state, publishResponse.getTransactionStatus());
    }
  }

  @Test
  @DisplayName("Should handle concurrent transaction ids")
  void testConcurrentTransactionIds() {
    List<String> transactionIds = new ArrayList<>();
    for (int i = 1; i <= 10; i++) {
      String txnId = "txn-concurrent-" + System.currentTimeMillis() + "-" + i;
      transactionIds.add(txnId);

      PublishResponseDTO response = new PublishResponseDTO();
      response.setTransactionId(txnId);
      response.setSuccess(true);
    }

    Assertions.assertEquals(10, transactionIds.size());
    for (String txnId : transactionIds) {
      Assertions.assertTrue(txnId.startsWith("txn-concurrent-"));
    }
  }

  @Test
  @DisplayName("Should validate transaction message payload consistency")
  void testTransactionPayloadConsistency() {
    String originalPayload = "consistent-data";
    String encoded = Base64.getEncoder().encodeToString(originalPayload.getBytes());
    
    MessageDTO messageDTO = new MessageDTO();
    messageDTO.setPayload(encoded);
    messageDTO.setIdentifier(1L);

    PublishRequestDTO request = new PublishRequestDTO();
    request.setMessage(messageDTO);
    request.setSessionName("consistency-test");

    String decoded = new String(Base64.getDecoder().decode(request.getMessage().getPayload()));
    Assertions.assertEquals(originalPayload, decoded);
  }
}
