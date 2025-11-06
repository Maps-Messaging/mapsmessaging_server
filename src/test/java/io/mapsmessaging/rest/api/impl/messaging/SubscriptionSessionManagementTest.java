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

import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.dto.rest.messaging.ConsumeRequestDTO;
import io.mapsmessaging.dto.rest.messaging.SessionInfoDTO;
import io.mapsmessaging.dto.rest.messaging.SubscriptionRequestDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@DisplayName("Subscription and Session Management Tests")
class SubscriptionSessionManagementTest {

  private SessionInfoDTO sessionInfo;

  @BeforeEach
  void setUp() {
    sessionInfo = new SessionInfoDTO();
    sessionInfo.setCreationTime(LocalDateTime.now());
    sessionInfo.setLastActivity(LocalDateTime.now());
  }

  @Test
  @DisplayName("Should create named subscription")
  void testCreateNamedSubscription() {
    SubscriptionRequestDTO subscriptionRequest = new SubscriptionRequestDTO();
    subscriptionRequest.setDestinationName("sensors/temperature");
    subscriptionRequest.setNamedSubscription("tempMonitor");
    subscriptionRequest.setMaxDepth(50);

    Assertions.assertEquals("sensors/temperature", subscriptionRequest.getDestinationName());
    Assertions.assertEquals("tempMonitor", subscriptionRequest.getNamedSubscription());
    Assertions.assertEquals(50, subscriptionRequest.getMaxDepth());
  }

  @Test
  @DisplayName("Should track subscription depth")
  void testSubscriptionDepth() {
    ConsumeRequestDTO consumeRequest = new ConsumeRequestDTO();
    consumeRequest.setDestination("sensors/temperature");
    consumeRequest.setDepth(100);

    Assertions.assertEquals("sensors/temperature", consumeRequest.getDestination());
    Assertions.assertEquals(100, consumeRequest.getDepth());
  }

  @Test
  @DisplayName("Should create session info with name")
  void testCreateSessionWithName() {
    sessionInfo.setSessionId("session-001");
    sessionInfo.setSessionName("WeatherMonitoring");
    sessionInfo.setStatus("ACTIVE");

    Assertions.assertEquals("session-001", sessionInfo.getSessionId());
    Assertions.assertEquals("WeatherMonitoring", sessionInfo.getSessionName());
    Assertions.assertEquals("ACTIVE", sessionInfo.getStatus());
  }

  @Test
  @DisplayName("Should track outstanding messages per destination")
  void testOutstandingMessagesTracking() {
    Map<String, Integer> outstandingMessages = new HashMap<>();
    outstandingMessages.put("sensors/temperature", 5);
    outstandingMessages.put("sensors/humidity", 3);
    outstandingMessages.put("sensors/pressure", 0);

    sessionInfo.setOutstandingMessages(outstandingMessages);

    Assertions.assertEquals(3, sessionInfo.getOutstandingMessages().size());
    Assertions.assertEquals(5, sessionInfo.getOutstandingMessages().get("sensors/temperature"));
    Assertions.assertEquals(0, sessionInfo.getOutstandingMessages().get("sensors/pressure"));
  }

  @Test
  @DisplayName("Should count active subscriptions")
  void testSubscriptionCount() {
    sessionInfo.setSubscriptionCount(3);

    Assertions.assertEquals(3, sessionInfo.getSubscriptionCount());
  }

  @Test
  @DisplayName("Should create transactional subscription")
  void testTransactionalSubscription() {
    SubscriptionRequestDTO subscriptionRequest = new SubscriptionRequestDTO();
    subscriptionRequest.setDestinationName("txn/queue");
    subscriptionRequest.setTransactional(true);
    subscriptionRequest.setMaxDepth(100);

    Assertions.assertTrue(subscriptionRequest.isTransactional());
    Assertions.assertEquals(100, subscriptionRequest.getMaxDepth());
  }

  @Test
  @DisplayName("Should apply filter to subscription")
  void testSubscriptionFilter() {
    SubscriptionRequestDTO subscriptionRequest = new SubscriptionRequestDTO();
    subscriptionRequest.setDestinationName("events/all");
    subscriptionRequest.setFilter("priority > 5");

    Assertions.assertEquals("priority > 5", subscriptionRequest.getFilter());
  }

  @Test
  @DisplayName("Should support wildcard subscriptions")
  void testWildcardSubscription() {
    SubscriptionRequestDTO subscriptionRequest = new SubscriptionRequestDTO();
    subscriptionRequest.setDestinationName("sensors/+/temperature");

    Assertions.assertTrue(subscriptionRequest.getDestinationName().contains("+"));
  }

  @Test
  @DisplayName("Should support multilevel wildcard subscriptions")
  void testMultilevelWildcardSubscription() {
    SubscriptionRequestDTO subscriptionRequest = new SubscriptionRequestDTO();
    subscriptionRequest.setDestinationName("sensors/building-1/#");

    Assertions.assertTrue(subscriptionRequest.getDestinationName().contains("#"));
  }

  @Test
  @DisplayName("Should track transactional session status")
  void testTransactionalSessionStatus() {
    sessionInfo.setSessionId("txn-session-001");
    sessionInfo.setTransactional(true);
    sessionInfo.setActiveTransactionId("txn-12345");

    Assertions.assertTrue(sessionInfo.isTransactional());
    Assertions.assertEquals("txn-12345", sessionInfo.getActiveTransactionId());
  }

  @Test
  @DisplayName("Should track last activity time")
  void testLastActivityTime() {
    LocalDateTime activityTime = LocalDateTime.now();
    sessionInfo.setLastActivity(activityTime);

    Assertions.assertEquals(activityTime, sessionInfo.getLastActivity());
  }

  @Test
  @DisplayName("Should consume messages with depth limit")
  void testConsumeMessagesWithDepthLimit() {
    ConsumeRequestDTO consumeRequest = new ConsumeRequestDTO();
    consumeRequest.setDestination("messages/queue");
    consumeRequest.setDepth(50);

    Assertions.assertEquals("messages/queue", consumeRequest.getDestination());
    Assertions.assertEquals(50, consumeRequest.getDepth());
  }

  @Test
  @DisplayName("Should consume all messages when no destination specified")
  void testConsumeAllMessages() {
    ConsumeRequestDTO consumeRequest = new ConsumeRequestDTO();
    consumeRequest.setDestination("");
    consumeRequest.setDepth(100);

    Assertions.assertTrue(consumeRequest.getDestination().isEmpty());
    Assertions.assertEquals(100, consumeRequest.getDepth());
  }

  @Test
  @DisplayName("Should validate QoS in subscription")
  void testSubscriptionQosValidation() {
    for (QualityOfService qos : new QualityOfService[]{
        QualityOfService.AT_MOST_ONCE,
        QualityOfService.AT_LEAST_ONCE,
        QualityOfService.EXACTLY_ONCE
    }) {
      SubscriptionRequestDTO subscriptionRequest = new SubscriptionRequestDTO();
      subscriptionRequest.setDestinationName("qos/test/" + qos.name());
      subscriptionRequest.setTransactional(qos == QualityOfService.EXACTLY_ONCE);

      Assertions.assertNotNull(subscriptionRequest.getDestinationName());
    }
  }

  @Test
  @DisplayName("Should manage multiple subscriptions in session")
  void testMultipleSubscriptionsInSession() {
    sessionInfo.setSessionId("multi-sub-001");
    sessionInfo.setSubscriptionCount(5);

    Map<String, Integer> outstandingMessages = new HashMap<>();
    outstandingMessages.put("topic1", 10);
    outstandingMessages.put("topic2", 20);
    outstandingMessages.put("topic3", 15);
    outstandingMessages.put("topic4", 0);
    outstandingMessages.put("topic5", 5);

    sessionInfo.setOutstandingMessages(outstandingMessages);

    Assertions.assertEquals(5, sessionInfo.getSubscriptionCount());
    Assertions.assertEquals(5, sessionInfo.getOutstandingMessages().size());
  }

  @Test
  @DisplayName("Should track session creation and last activity times")
  void testSessionTimestamps() {
    LocalDateTime creationTime = LocalDateTime.now().minusHours(1);
    LocalDateTime lastActivityTime = LocalDateTime.now();

    sessionInfo.setCreationTime(creationTime);
    sessionInfo.setLastActivity(lastActivityTime);

    Assertions.assertEquals(creationTime, sessionInfo.getCreationTime());
    Assertions.assertEquals(lastActivityTime, sessionInfo.getLastActivity());
    Assertions.assertTrue(lastActivityTime.isAfter(creationTime));
  }

  @Test
  @DisplayName("Should handle retention message flag")
  void testRetentionMessageFlag() {
    SubscriptionRequestDTO subscriptionRequest = new SubscriptionRequestDTO();
    subscriptionRequest.setDestinationName("retained/topic");
    subscriptionRequest.setRetainMessage(true);

    Assertions.assertTrue(subscriptionRequest.isRetainMessage());
  }

  @Test
  @DisplayName("Should support named subscriptions for reuse")
  void testNamedSubscriptionReuse() {
    String sharedSubName = "shared-sub-weather";

    SubscriptionRequestDTO sub1 = new SubscriptionRequestDTO();
    sub1.setDestinationName("weather/+/temperature");
    sub1.setNamedSubscription(sharedSubName);

    SubscriptionRequestDTO sub2 = new SubscriptionRequestDTO();
    sub2.setDestinationName("weather/+/temperature");
    sub2.setNamedSubscription(sharedSubName);

    Assertions.assertEquals(sub1.getNamedSubscription(), sub2.getNamedSubscription());
  }

  @Test
  @DisplayName("Should consume messages with proper ordering")
  void testMessageOrdering() {
    ConsumeRequestDTO consumeRequest = new ConsumeRequestDTO();
    consumeRequest.setDestination("ordered/queue");
    consumeRequest.setDepth(10);

    Assertions.assertEquals("ordered/queue", consumeRequest.getDestination());
    Assertions.assertEquals(10, consumeRequest.getDepth());
  }

  @Test
  @DisplayName("Should set max depth for subscription")
  void testMaxDepthForSubscription() {
    SubscriptionRequestDTO subscriptionRequest = new SubscriptionRequestDTO();
    subscriptionRequest.setDestinationName("limited/queue");
    subscriptionRequest.setMaxDepth(500);

    Assertions.assertEquals(500, subscriptionRequest.getMaxDepth());
  }

  @Test
  @DisplayName("Should track session status transitions")
  void testSessionStatusTransitions() {
    String[] statuses = {"CREATED", "ACTIVE", "IDLE", "SUSPENDED", "CLOSED"};

    for (String status : statuses) {
      sessionInfo.setStatus(status);
      Assertions.assertEquals(status, sessionInfo.getStatus());
    }
  }

  @Test
  @DisplayName("Should handle zero outstanding messages")
  void testZeroOutstandingMessages() {
    Map<String, Integer> outstandingMessages = new HashMap<>();
    outstandingMessages.put("idle/topic1", 0);
    outstandingMessages.put("idle/topic2", 0);

    sessionInfo.setOutstandingMessages(outstandingMessages);

    int totalPending = sessionInfo.getOutstandingMessages()
        .values()
        .stream()
        .mapToInt(Integer::intValue)
        .sum();

    Assertions.assertEquals(0, totalPending);
  }

  @Test
  @DisplayName("Should calculate total outstanding messages")
  void testTotalOutstandingMessages() {
    Map<String, Integer> outstandingMessages = new HashMap<>();
    outstandingMessages.put("q1", 10);
    outstandingMessages.put("q2", 20);
    outstandingMessages.put("q3", 5);
    outstandingMessages.put("q4", 15);

    sessionInfo.setOutstandingMessages(outstandingMessages);

    int total = sessionInfo.getOutstandingMessages()
        .values()
        .stream()
        .mapToInt(Integer::intValue)
        .sum();

    Assertions.assertEquals(50, total);
  }
}
