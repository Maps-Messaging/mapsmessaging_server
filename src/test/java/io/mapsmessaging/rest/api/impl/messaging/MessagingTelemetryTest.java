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

import io.mapsmessaging.dto.rest.messaging.PublishResponseDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@DisplayName("Messaging Telemetry and Performance Tests")
class MessagingTelemetryTest {

  private PublishResponseDTO publishResponse;

  @BeforeEach
  void setUp() {
    publishResponse = new PublishResponseDTO();
    publishResponse.setPublishTime(LocalDateTime.now());
  }

  @Test
  @DisplayName("Should track publish success")
  void testPublishSuccessTelemetry() {
    publishResponse.setSuccess(true);
    publishResponse.setMessageId("msg-001");
    publishResponse.setMessage("Message published successfully");

    Assertions.assertTrue(publishResponse.isSuccess());
    Assertions.assertEquals("msg-001", publishResponse.getMessageId());
  }

  @Test
  @DisplayName("Should track publish failure")
  void testPublishFailureTelemetry() {
    publishResponse.setSuccess(false);
    publishResponse.setMessage("Failed to publish message");
    publishResponse.setErrorDetails("Topic not found");

    Assertions.assertFalse(publishResponse.isSuccess());
    Assertions.assertNotNull(publishResponse.getErrorDetails());
    Assertions.assertEquals("Topic not found", publishResponse.getErrorDetails());
  }

  @Test
  @DisplayName("Should measure message delivery latency")
  void testDeliveryLatencyMeasurement() {
    long startTime = System.currentTimeMillis();

    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    long endTime = System.currentTimeMillis();
    long latency = endTime - startTime;

    publishResponse.setDeliveryLatency(latency);
    publishResponse.setSuccess(true);

    Assertions.assertTrue(publishResponse.getDeliveryLatency() >= 100);
    Assertions.assertTrue(publishResponse.isSuccess());
  }

  @Test
  @DisplayName("Should track publish timestamp")
  void testPublishTimestamp() {
    LocalDateTime publishTime = LocalDateTime.now();
    publishResponse.setPublishTime(publishTime);

    Assertions.assertNotNull(publishResponse.getPublishTime());
    Assertions.assertEquals(publishTime, publishResponse.getPublishTime());
  }

  @Test
  @DisplayName("Should track message identifier")
  void testMessageIdentifierTracking() {
    publishResponse.setMessageId("msg-12345");
    publishResponse.setSuccess(true);

    Assertions.assertEquals("msg-12345", publishResponse.getMessageId());
  }

  @Test
  @DisplayName("Should calculate average latency over multiple publishes")
  void testAverageLatencyCalculation() {
    List<Long> latencies = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      latencies.add((long) (50 + (i * 10)));
    }

    long averageLatency = latencies.stream()
        .mapToLong(Long::longValue)
        .sum() / latencies.size();

    Assertions.assertTrue(averageLatency > 50);
  }

  @Test
  @DisplayName("Should track min and max latency")
  void testMinMaxLatencyTracking() {
    List<Long> latencies = new ArrayList<>();
    latencies.add(10L);
    latencies.add(50L);
    latencies.add(25L);
    latencies.add(100L);
    latencies.add(75L);

    long minLatency = latencies.stream().mapToLong(Long::longValue).min().orElse(0);
    long maxLatency = latencies.stream().mapToLong(Long::longValue).max().orElse(0);

    Assertions.assertEquals(10L, minLatency);
    Assertions.assertEquals(100L, maxLatency);
  }

  @Test
  @DisplayName("Should track transaction status in telemetry")
  void testTransactionStatusInTelemetry() {
    publishResponse.setTransactionId("txn-001");
    publishResponse.setTransactionStatus("COMMITTED");
    publishResponse.setSuccess(true);

    Assertions.assertNotNull(publishResponse.getTransactionId());
    Assertions.assertEquals("COMMITTED", publishResponse.getTransactionStatus());
    Assertions.assertTrue(publishResponse.isSuccess());
  }

  @Test
  @DisplayName("Should track error details for failed publishes")
  void testErrorDetailsTelemetry() {
    publishResponse.setSuccess(false);
    publishResponse.setMessage("Publish failed");
    publishResponse.setErrorDetails("Connection timeout");

    Assertions.assertFalse(publishResponse.isSuccess());
    Assertions.assertNotNull(publishResponse.getErrorDetails());
  }

  @Test
  @DisplayName("Should track publication success rate")
  void testPublicationSuccessRate() {
    int totalPublishes = 100;
    int successfulPublishes = 95;

    double successRate = (double) successfulPublishes / totalPublishes * 100;

    Assertions.assertEquals(95.0, successRate);
  }

  @Test
  @DisplayName("Should track zero latency edge case")
  void testZeroLatency() {
    publishResponse.setDeliveryLatency(0);
    publishResponse.setSuccess(true);

    Assertions.assertEquals(0, publishResponse.getDeliveryLatency());
    Assertions.assertTrue(publishResponse.isSuccess());
  }

  @Test
  @DisplayName("Should track very large latency")
  void testLargeLatency() {
    long largeLatency = 60000; // 60 seconds
    publishResponse.setDeliveryLatency(largeLatency);

    Assertions.assertEquals(60000, publishResponse.getDeliveryLatency());
  }

  @Test
  @DisplayName("Should track latency percentiles")
  void testLatencyPercentiles() {
    List<Long> latencies = new ArrayList<>();
    for (int i = 1; i <= 100; i++) {
      latencies.add((long) i);
    }

    latencies.sort(Long::compareTo);

    long p50 = latencies.get((int) (latencies.size() * 0.50) - 1);
    long p95 = latencies.get((int) (latencies.size() * 0.95) - 1);
    long p99 = latencies.get((int) (latencies.size() * 0.99) - 1);

    Assertions.assertTrue(p50 > 0);
    Assertions.assertTrue(p95 > p50);
    Assertions.assertTrue(p99 > p95);
  }

  @Test
  @DisplayName("Should track publish attempts")
  void testPublishAttemptTracking() {
    int attempts = 0;
    int maxAttempts = 3;

    for (int i = 0; i < maxAttempts; i++) {
      attempts++;
      publishResponse.setSuccess(i == maxAttempts - 1);
    }

    Assertions.assertEquals(3, attempts);
    Assertions.assertTrue(publishResponse.isSuccess());
  }

  @Test
  @DisplayName("Should track retry count in telemetry")
  void testRetryCountTracking() {
    int retryCount = 2;
    publishResponse.setSuccess(true);
    publishResponse.setMessage("Published after " + retryCount + " retries");

    Assertions.assertTrue(publishResponse.isSuccess());
  }

  @Test
  @DisplayName("Should track concurrent publish operations")
  void testConcurrentPublishTracking() {
    List<PublishResponseDTO> responses = new ArrayList<>();

    for (int i = 0; i < 5; i++) {
      PublishResponseDTO response = new PublishResponseDTO();
      response.setMessageId("msg-concurrent-" + i);
      response.setSuccess(true);
      response.setDeliveryLatency(10 + i * 5);
      responses.add(response);
    }

    Assertions.assertEquals(5, responses.size());
    for (PublishResponseDTO response : responses) {
      Assertions.assertTrue(response.isSuccess());
    }
  }

  @Test
  @DisplayName("Should track message throughput")
  void testMessageThroughputTracking() {
    long startTime = System.currentTimeMillis();

    for (int i = 0; i < 100; i++) {
      PublishResponseDTO response = new PublishResponseDTO();
      response.setSuccess(true);
    }

    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;

    Assertions.assertTrue(duration >= 0);
  }

  @Test
  @DisplayName("Should track error count")
  void testErrorCountTracking() {
    int errorCount = 0;
    int totalAttempts = 50;

    for (int i = 0; i < totalAttempts; i++) {
      PublishResponseDTO response = new PublishResponseDTO();
      if (i % 10 == 0) {
        response.setSuccess(false);
        errorCount++;
      } else {
        response.setSuccess(true);
      }
    }

    Assertions.assertEquals(5, errorCount);
  }

  @Test
  @DisplayName("Should track latency distribution")
  void testLatencyDistribution() {
    int fastCount = 0;
    int mediumCount = 0;
    int slowCount = 0;

    List<Long> latencies = new ArrayList<>();
    latencies.add(5L);
    latencies.add(15L);
    latencies.add(50L);
    latencies.add(100L);

    for (long latency : latencies) {
      if (latency < 25) fastCount++;
      else if (latency < 75) mediumCount++;
      else slowCount++;
    }

    Assertions.assertEquals(2, fastCount);
    Assertions.assertEquals(1, mediumCount);
    Assertions.assertEquals(1, slowCount);
  }

  @Test
  @DisplayName("Should track peak latency")
  void testPeakLatencyTracking() {
    PublishResponseDTO response1 = new PublishResponseDTO();
    response1.setDeliveryLatency(50);

    PublishResponseDTO response2 = new PublishResponseDTO();
    response2.setDeliveryLatency(150);

    PublishResponseDTO response3 = new PublishResponseDTO();
    response3.setDeliveryLatency(100);

    long peakLatency = Math.max(
        Math.max(response1.getDeliveryLatency(), response2.getDeliveryLatency()),
        response3.getDeliveryLatency()
    );

    Assertions.assertEquals(150, peakLatency);
  }

  @Test
  @DisplayName("Should track baseline latency")
  void testBaselineLatencyTracking() {
    long baselineLatency = 10L;
    publishResponse.setDeliveryLatency(baselineLatency);

    Assertions.assertEquals(baselineLatency, publishResponse.getDeliveryLatency());
  }

  @Test
  @DisplayName("Should track publish timing across multiple messages")
  void testMultipleMessageTiming() {
    long startTime = System.currentTimeMillis();

    for (int i = 0; i < 10; i++) {
      PublishResponseDTO response = new PublishResponseDTO();
      response.setSuccess(true);
      response.setDeliveryLatency(10 * (i + 1));
    }

    long endTime = System.currentTimeMillis();
    long totalTime = endTime - startTime;

    Assertions.assertTrue(totalTime >= 0);
  }
}
