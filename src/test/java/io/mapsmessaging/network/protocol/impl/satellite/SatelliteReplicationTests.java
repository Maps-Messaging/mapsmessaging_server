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
package io.mapsmessaging.network.protocol.impl.satellite;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.test.BaseTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SatelliteReplicationTests extends BaseTestConfig {

  private static final String ENTERPRISE_BASE = "/000000000SKYEE3D/maps/in";

  private static final String MODEM_TELEMETRY_BASE = "/sensors";
  private static final String ENTERPRISE_TELEMETRY_BASE = ENTERPRISE_BASE + "/satellite";

  private static final String MODEM_RAW_REQUEST_TEMPLATE = "/outbound/%d/%d";

  // NOTE: your config example/default says "/outbound" but field value says "/outgoing".
  // This test uses "/outgoing" because that's what you showed as the actual value.
  private static final String ENTERPRISE_RAW_RESPONSE = "/000000000SKYEE3D/common/in/20/7";

  private static final Duration OUTBOUND_POLL_INTERVAL = Duration.ofSeconds(2);
  private static final Duration INBOUND_POLL_INTERVAL = Duration.ofSeconds(5);
  private static final Duration WAIT = computeWait(OUTBOUND_POLL_INTERVAL, INBOUND_POLL_INTERVAL);

  private final String runId = UUID.randomUUID().toString();

  private Session modemSideSession;
  private Session enterpriseSideSession;

  @AfterEach
  void cleanup() {
    closeQuietly(modemSideSession);
    closeQuietly(enterpriseSideSession);
    modemSideSession = null;
    enterpriseSideSession = null;
  }

  @Test
  void dropSemanticsOnlyLastEventPerTopicIsObservedModemToEnterprise() throws Exception {
    TestReceiver enterpriseReceiver = new TestReceiver();
    TestReceiver modemReceiver = new TestReceiver();

    modemSideSession = createModemSideSession(modemReceiver.listener());
    enterpriseSideSession = createEnterpriseSideSession(enterpriseReceiver.listener());

    String modemPublishTopic = modemSensorsTopic(1);
    String enterpriseReceiveTopic = enterpriseSatelliteTopic(1);

    SubscriptionContext subscription = enterpriseReceiver.subscribe(enterpriseSideSession, enterpriseReceiveTopic);

    int publishCount = 25;
    for (int index = 0; index < publishCount; index++) {
      publishText(modemSideSession, modemPublishTopic, "msg-" + index);
    }

    boolean received = enterpriseReceiver.await(enterpriseReceiveTopic, WAIT);
    Assertions.assertTrue(received, "Expected at least one replicated event within " + WAIT);

    String lastPayload = enterpriseReceiver.getLastText(enterpriseReceiveTopic);
    Assertions.assertEquals("msg-" + (publishCount - 1), lastPayload, "Only the last event should be observed");

    enterpriseSideSession.removeSubscription(subscription.getKey());
  }

  @Test
  void tenTopicsProduceTenEventsOnePerTopicModemToEnterprise() throws Exception {
    TestReceiver enterpriseReceiver = new TestReceiver();
    TestReceiver modemReceiver = new TestReceiver();

    modemSideSession = createModemSideSession(modemReceiver.listener());
    enterpriseSideSession = createEnterpriseSideSession(enterpriseReceiver.listener());

    int topicCount = 10;
    SubscriptionContext[] subscriptions = new SubscriptionContext[topicCount];

    for (int topicIndex = 0; topicIndex < topicCount; topicIndex++) {
      subscriptions[topicIndex] = enterpriseReceiver.subscribe(enterpriseSideSession, enterpriseSatelliteTopic(topicIndex));
    }

    for (int topicIndex = 0; topicIndex < topicCount; topicIndex++) {
      String modemPublishTopic = modemSensorsTopic(topicIndex);

      for (int messageIndex = 0; messageIndex < 7; messageIndex++) {
        publishText(modemSideSession, modemPublishTopic, "t" + topicIndex + "-m" + messageIndex);
      }
    }

    boolean allReceived = enterpriseReceiver.awaitAll(topicCount, WAIT);
    Assertions.assertTrue(allReceived, "Expected 1 event per topic (" + topicCount + ") within " + WAIT);

    for (int topicIndex = 0; topicIndex < topicCount; topicIndex++) {
      String topic = enterpriseSatelliteTopic(topicIndex);
      String lastPayload = enterpriseReceiver.getLastText(topic);
      Assertions.assertEquals("t" + topicIndex + "-m6", lastPayload, "Last-per-topic must hold for " + topic);
    }

    for (SubscriptionContext subscription : subscriptions) {
      enterpriseSideSession.removeSubscription(subscription.getKey());
    }
  }

  @Test
  void rawNanoIoTPacketsAreRoutedUsingIncomingSinMinAndObservedOnOutgoing() throws Exception {
    TestReceiver enterpriseReceiver = new TestReceiver();
    TestReceiver modemReceiver = new TestReceiver();

    modemSideSession = createModemSideSession(modemReceiver.listener());
    enterpriseSideSession = createEnterpriseSideSession(enterpriseReceiver.listener());

    int sin = 20;
    int min = 7;

    String modemPublishTopic = modemRawRequestTopic(sin, min);
    String enterpriseReceiveTopic = ENTERPRISE_RAW_RESPONSE;

    SubscriptionContext subscription = enterpriseReceiver.subscribe(enterpriseSideSession, enterpriseReceiveTopic);

    byte[] raw = buildNanoIoTRawPacket(sin, min, "hello-nano");
    publishRaw(modemSideSession, modemPublishTopic, raw);

    boolean received = enterpriseReceiver.await(enterpriseReceiveTopic, WAIT);
    Assertions.assertTrue(received, "Expected raw packet arrival within " + WAIT);

    byte[] observed = enterpriseReceiver.getLastBytes(enterpriseReceiveTopic);
    Assertions.assertNotNull(observed, "Expected payload bytes");
    Assertions.assertTrue(observed.length >= 2, "Raw payload must be at least 2 bytes");
    Assertions.assertEquals((byte) sin, observed[0], "SIN must be first byte");
    Assertions.assertEquals((byte) min, observed[1], "MIN must be second byte");

    String tail = new String(observed, 2, observed.length - 2, StandardCharsets.UTF_8);
    Assertions.assertEquals("hello-nano", tail, "Raw payload tail must be preserved");

    enterpriseSideSession.removeSubscription(subscription.getKey());
  }
  @Test
  void computeNamespaceQueueDepthSixDeliversSixEventsPerTopic() throws Exception {
    MultiReceiver enterpriseReceiver = new MultiReceiver();
    TestReceiver modemReceiver = new TestReceiver();

    modemSideSession = createModemSideSession(modemReceiver.listener());
    enterpriseSideSession = createEnterpriseSideSession(enterpriseReceiver.listener());

    String modemPublishTopic = modemComputeTopic(0);
    String enterpriseReceiveTopic = enterpriseComputeMappedTopic(0);

    int queueDepth = 6;

    SubscriptionContext subscription = enterpriseReceiver.subscribeExpecting(enterpriseSideSession, enterpriseReceiveTopic, queueDepth);

    for (int index = 0; index < queueDepth; index++) {
      publishText(modemSideSession, modemPublishTopic, "c-" + index);
    }

    boolean receivedSix = enterpriseReceiver.await(enterpriseReceiveTopic, WAIT);
    Assertions.assertTrue(receivedSix, "Expected " + queueDepth + " replicated events within " + WAIT);

    Assertions.assertEquals(queueDepth, enterpriseReceiver.getCount(enterpriseReceiveTopic), "Expected exactly " + queueDepth + " events");

    for (int index = 0; index < queueDepth; index++) {
      Assertions.assertEquals("c-" + index, enterpriseReceiver.getText(enterpriseReceiveTopic, index), "Event order mismatch at index " + index);
    }

    enterpriseSideSession.removeSubscription(subscription.getKey());
  }

  @Test
  void computeNamespaceQueueDepthSixDropsBeyondSixEventsPerTopic() throws Exception {
    MultiReceiver enterpriseReceiver = new MultiReceiver();
    TestReceiver modemReceiver = new TestReceiver();

    modemSideSession = createModemSideSession(modemReceiver.listener());
    enterpriseSideSession = createEnterpriseSideSession(enterpriseReceiver.listener());

    String modemPublishTopic = modemComputeTopic(1);
    String enterpriseReceiveTopic = enterpriseComputeMappedTopic(1);

    int queueDepth = 6;
    int publishCount = 10;

    SubscriptionContext subscription = enterpriseReceiver.subscribeExpecting(enterpriseSideSession, enterpriseReceiveTopic, queueDepth);

    for (int index = 0; index < publishCount; index++) {
      publishText(modemSideSession, modemPublishTopic, "c-" + index);
    }

    boolean receivedSix = enterpriseReceiver.await(enterpriseReceiveTopic, WAIT);
    Assertions.assertTrue(receivedSix, "Expected " + queueDepth + " replicated events within " + WAIT);

    int observed = enterpriseReceiver.getCount(enterpriseReceiveTopic);
    Assertions.assertEquals(queueDepth, observed, "Expected queue depth cap of " + queueDepth + " events, observed=" + observed);

    enterpriseSideSession.removeSubscription(subscription.getKey());
  }


  @Test
  void statsAnalyticsEmitsAggregatedAdvancedStatsAfter100Events() throws Exception {
    TestReceiver enterpriseReceiver = new TestReceiver();
    TestReceiver modemReceiver = new TestReceiver();

    modemSideSession = createModemSideSession(modemReceiver.listener());
    enterpriseSideSession = createEnterpriseSideSession(enterpriseReceiver.listener());

    String modemStatsTopic = modemStatsTopic(0);
    String enterpriseAggregatedTopic = enterpriseStatsMappedTopic(0);

    SubscriptionContext subscription = enterpriseReceiver.subscribe(enterpriseSideSession, enterpriseAggregatedTopic);

    int eventCount = 100;
    for (int index = 0; index < eventCount; index++) {
      double value1 = index;
      double value2 = index * 2.0;

      JsonObject event = new JsonObject();
      event.addProperty("value1", value1);
      event.addProperty("value2", value2);

      publishText(modemSideSession, modemStatsTopic, event.toString());
    }

    boolean received = enterpriseReceiver.await(enterpriseAggregatedTopic, WAIT);
    Assertions.assertTrue(received, "Expected aggregated stats event within " + WAIT);

    String payload = enterpriseReceiver.getLastText(enterpriseAggregatedTopic);

    JsonElement root = JsonParser.parseString(payload);
    Assertions.assertTrue(root.isJsonObject(), "Aggregated payload must be JSON object: " + payload);

    JsonObject aggregated = root.getAsJsonObject();

    JsonObject stats1 = getObject(aggregated, "value1", payload);
    JsonObject stats2 = getObject(aggregated, "value2", payload);

    assertAdvancedStats(stats1, 0.0, 99.0, 0.0, 99.0, 49.5, 100, 1.0, -1.0, payload);
    assertAdvancedStats(stats2, 0.0, 198.0, 0.0, 198.0, 99.0, 100, 2.0, -2.0, payload);

    enterpriseSideSession.removeSubscription(subscription.getKey());
  }

  private static JsonObject getObject(JsonObject root, String name, String payload) {
    Assertions.assertTrue(root.has(name), "Missing '" + name + "' in payload: " + payload);
    JsonElement element = root.get(name);
    Assertions.assertTrue(element.isJsonObject(), "'" + name + "' must be an object. Payload: " + payload);
    return element.getAsJsonObject();
  }

  private static void assertAdvancedStats(
      JsonObject stats,
      double expectedFirst,
      double expectedLast,
      double expectedMin,
      double expectedMax,
      double expectedAverage,
      int expectedCount,
      double expectedSlope,
      double expectedIntercept,
      String payload
  ) {
    assertNumberNear(stats, "first", expectedFirst, 0.000001, payload);
    assertNumberNear(stats, "last", expectedLast, 0.000001, payload);
    assertNumberNear(stats, "min", expectedMin, 0.000001, payload);
    assertNumberNear(stats, "max", expectedMax, 0.000001, payload);
    assertNumberNear(stats, "average", expectedAverage, 0.000001, payload);

    Assertions.assertTrue(stats.has("count"), "Missing 'count'. Payload: " + payload);
    Assertions.assertEquals(expectedCount, stats.get("count").getAsInt(), "count mismatch. Payload: " + payload);

    Assertions.assertTrue(stats.has("mismatched"), "Missing 'mismatched'. Payload: " + payload);
    Assertions.assertEquals(0, stats.get("mismatched").getAsInt(), "mismatched must be 0. Payload: " + payload);

    Assertions.assertTrue(stats.has("stdDev"), "Missing 'stdDev' (AdvancedStatistics). Payload: " + payload);
    Assertions.assertTrue(stats.get("stdDev").getAsDouble() > 0.0, "stdDev must be > 0. Payload: " + payload);

    assertNumberNear(stats, "slope", expectedSlope, 0.000001, payload);
    assertNumberNear(stats, "intercept", expectedIntercept, 0.000001, payload);

    Assertions.assertTrue(stats.has("firstUpdateMillis"), "Missing 'firstUpdateMillis'. Payload: " + payload);
    Assertions.assertTrue(stats.has("lastUpdateMillis"), "Missing 'lastUpdateMillis'. Payload: " + payload);
    Assertions.assertTrue(stats.get("firstUpdateMillis").getAsLong() > 0L, "firstUpdateMillis must be set. Payload: " + payload);
    Assertions.assertTrue(stats.get("lastUpdateMillis").getAsLong() > 0L, "lastUpdateMillis must be set. Payload: " + payload);
  }

  private static void assertNumberNear(JsonObject o, String field, double expected, double delta, String payload) {
    Assertions.assertTrue(o.has(field), "Missing '" + field + "'. Payload: " + payload);
    Assertions.assertEquals(expected, o.get(field).getAsDouble(), delta, "Mismatch for '" + field + "'. Payload: " + payload);
  }

  private String modemComputeTopic(int topicIndex) {
    return  "/compute/" + runId + "/t/" + topicIndex;
  }

  private String modemStatsTopic(int topicIndex) {
    return "/stats/" + runId + "/t/" + topicIndex;
  }

  private String enterpriseComputeMappedTopic(int topicIndex) {
    return ENTERPRISE_BASE+"/remote/compute/" + runId + "/t/" + topicIndex;
  }

  private static class MultiReceiver {

    private final Map<String, java.util.List<Message>> messagesByTopic;
    private final Map<String, CountDownLatch> latchByTopic;

    private MultiReceiver() {
      this.messagesByTopic = new ConcurrentHashMap<>();
      this.latchByTopic = new ConcurrentHashMap<>();
    }

    private MessageListener listener() {
      return messageEvent -> {
        String destination = messageEvent.getDestinationName();
        Message message = messageEvent.getMessage();
        if (destination != null && message != null) {
          messagesByTopic.computeIfAbsent(destination, key -> java.util.Collections.synchronizedList(new java.util.ArrayList<>()))
              .add(message);

          CountDownLatch latch = latchByTopic.get(destination);
          if (latch != null) {
            latch.countDown();
          }
        }

        messageEvent.getCompletionTask().run();
      };
    }

    private SubscriptionContext subscribeExpecting(Session session, String topic, int expectedCount) throws Exception {
      latchByTopic.put(topic, new CountDownLatch(expectedCount));

      SubscriptionContextBuilder builder = new SubscriptionContextBuilder(topic, ClientAcknowledgement.AUTO);
      SubscriptionContext context = builder
          .setQos(QualityOfService.AT_MOST_ONCE)
          .setReceiveMaximum(100)
          .build();

      session.addSubscription(context);
      return context;
    }

    private boolean await(String topic, Duration timeout) throws InterruptedException {
      CountDownLatch latch = latchByTopic.get(topic);
      if (latch == null) {
        throw new IllegalStateException("No latch registered for topic " + topic);
      }
      return latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    private int getCount(String topic) {
      java.util.List<Message> list = messagesByTopic.get(topic);
      if (list == null) {
        return 0;
      }
      return list.size();
    }

    private String getText(String topic, int index) {
      java.util.List<Message> list = messagesByTopic.get(topic);
      Assertions.assertNotNull(list, "No messages recorded for topic " + topic);
      Assertions.assertTrue(index >= 0 && index < list.size(), "Index out of bounds. topic=" + topic + " index=" + index + " size=" + list.size());

      Message message = list.get(index);
      byte[] bytes = message.getOpaqueData();
      Assertions.assertNotNull(bytes, "Message has null payload for " + topic);

      return new String(bytes, StandardCharsets.UTF_8);
    }
  }

  private String enterpriseStatsMappedTopic(int topicIndex) {
    return ENTERPRISE_BASE+"/remote/stats/" + runId + "/t/" + topicIndex;
  }


  private Session createModemSideSession(MessageListener listener) throws LoginException, IOException {
    return createSession("SatelliteModemSide" + System.nanoTime(), 60, 60, false, listener);
  }

  private Session createEnterpriseSideSession(MessageListener listener) throws LoginException, IOException {
    return createSession("SatelliteEnterpriseSide" + System.nanoTime(), 60, 60, false, listener);
  }

  private static void publishText(Session session, String topic, String payload) throws Exception {
    Objects.requireNonNull(session, "session");
    Objects.requireNonNull(topic, "topic");
    Objects.requireNonNull(payload, "payload");

    publishRaw(session, topic, payload.getBytes(StandardCharsets.UTF_8));
  }

  private static void publishRaw(Session session, String topic, byte[] payload) throws Exception {
    Objects.requireNonNull(session, "session");
    Objects.requireNonNull(topic, "topic");
    Objects.requireNonNull(payload, "payload");

    MessageBuilder messageBuilder = new MessageBuilder();
    Message message = messageBuilder.setOpaqueData(payload).build();

    session.findDestination(topic, DestinationType.TOPIC).get().storeMessage(message);
  }

  private static byte[] buildNanoIoTRawPacket(int sin, int min, String tailText) {
    byte[] tailBytes = tailText.getBytes(StandardCharsets.UTF_8);
    ByteBuffer buffer = ByteBuffer.allocate(2 + tailBytes.length);
    buffer.put((byte) (sin & 0xFF));
    buffer.put((byte) (min & 0xFF));
    buffer.put(tailBytes);
    return buffer.array();
  }

  private String modemSensorsTopic(int topicIndex) {
    return MODEM_TELEMETRY_BASE + "/" + runId + "/t/" + topicIndex;
  }

  private String enterpriseSatelliteTopic(int topicIndex) {
    return ENTERPRISE_TELEMETRY_BASE + "/" + runId + "/t/" + topicIndex;
  }

  private String modemRawRequestTopic(int sin, int min) {
    return String.format(MODEM_RAW_REQUEST_TEMPLATE, sin, min);
  }

  private void closeQuietly(Session session) {
    if (session == null) {
      return;
    }
    try {
      close(session);
    } catch (Exception ignored) {
    }
  }

  private static Duration computeWait(Duration outboundPoll, Duration inboundPoll) {
    Duration slowest = outboundPoll.compareTo(inboundPoll) > 0 ? outboundPoll : inboundPoll;

    long scaledSeconds = slowest.toSeconds() * 8L;
    if (scaledSeconds < 30L) {
      scaledSeconds = 30L;
    }
    if (scaledSeconds > 120L) {
      scaledSeconds = 120L;
    }
    return Duration.ofSeconds(scaledSeconds);
  }

  private static class TestReceiver {

    private final Map<String, Message> lastByTopic;
    private final Map<String, CountDownLatch> latchByTopic;

    private TestReceiver() {
      this.lastByTopic = new ConcurrentHashMap<>();
      this.latchByTopic = new ConcurrentHashMap<>();
    }

    private MessageListener listener() {
      return messageEvent -> {
        String destination = messageEvent.getDestinationName();
        Message message = messageEvent.getMessage();

        if (destination != null && message != null) {
          lastByTopic.put(destination, message);

          CountDownLatch latch = latchByTopic.get(destination);
          if (latch != null) {
            latch.countDown();
          }
        }

        messageEvent.getCompletionTask().run();
      };
    }

    private SubscriptionContext subscribe(Session session, String topic) throws Exception {
      latchByTopic.put(topic, new CountDownLatch(1));

      SubscriptionContextBuilder builder = new SubscriptionContextBuilder(topic, ClientAcknowledgement.AUTO);
      SubscriptionContext context = builder
          .setQos(QualityOfService.AT_MOST_ONCE)
          .setReceiveMaximum(100)
          .build();

      session.addSubscription(context);
      return context;
    }

    private boolean await(String topic, Duration timeout) throws InterruptedException {
      CountDownLatch latch = latchByTopic.get(topic);
      if (latch == null) {
        throw new IllegalStateException("No latch registered for topic " + topic);
      }
      return latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    private boolean awaitAll(int expectedTopics, Duration timeout) throws InterruptedException {
      long deadline = System.nanoTime() + timeout.toNanos();
      for (CountDownLatch latch : latchByTopic.values()) {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) {
          return false;
        }
        if (!latch.await(remaining, TimeUnit.NANOSECONDS)) {
          return false;
        }
      }
      return lastByTopic.size() >= expectedTopics;
    }

    private String getLastText(String topic) {
      Message message = lastByTopic.get(topic);
      Assertions.assertNotNull(message, "No message recorded for topic " + topic);

      byte[] bytes = message.getOpaqueData();
      Assertions.assertNotNull(bytes, "Message has null payload for " + topic);

      return new String(bytes, StandardCharsets.UTF_8);
    }

    private byte[] getLastBytes(String topic) {
      Message message = lastByTopic.get(topic);
      Assertions.assertNotNull(message, "No message recorded for topic " + topic);

      byte[] bytes = message.getOpaqueData();
      Assertions.assertNotNull(bytes, "Message has null payload for " + topic);

      return bytes;
    }
  }
}