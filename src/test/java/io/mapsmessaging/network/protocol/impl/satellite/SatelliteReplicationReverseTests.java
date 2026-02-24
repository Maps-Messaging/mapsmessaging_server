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
import org.junit.jupiter.api.*;

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

class SatelliteReplicationReverseTests extends BaseTestConfig {

  private static boolean isInitialised = false;

  // -----------------------------
  // REST/Enterprise -> Modem topics
  // -----------------------------
  //
  // Replace these once you confirm your reverse mapping endpoints.
  //
  // Example idea (NOT assumed correct):
  //   ENTERPRISE_TO_MODEM_TELEMETRY_BASE might be: "/000.../maps/out/satellite"
  //   MODEM_RECEIVE_TELEMETRY_BASE might be: "/sensors"
  //
  private static final String ENTERPRISE_BASE = "/000000000SKYEE3D";

  private static final String ENTERPRISE_TO_MODEM_TELEMETRY_BASE = ENTERPRISE_BASE + "/maps/out/satellite";
  private static final String MODEM_RECEIVE_TELEMETRY_BASE = "/satellite";

  // Raw reverse mapping endpoints (you will replace these):
  // Enterprise publishes raw -> modem receives at SIN/MIN request topic.
  private static final String ENTERPRISE_TO_MODEM_RAW_PUBLISH_TEMPLATE = ENTERPRISE_BASE + "/common/out/%d/%d";
  private static final String MODEM_RAW_RECEIVE_TEMPLATE = "/incoming/%d/%d";

  private static final Duration OUTBOUND_POLL_INTERVAL = Duration.ofSeconds(2);
  private static final Duration INBOUND_POLL_INTERVAL = Duration.ofSeconds(5);
  private static final Duration WAIT = computeWait(OUTBOUND_POLL_INTERVAL, INBOUND_POLL_INTERVAL);

  private final String runId = UUID.randomUUID().toString();

  private Session modemSideSession;
  private Session enterpriseSideSession;

  @BeforeEach
  void checkForLinkUp(){
    if(!isInitialised){
      delay(5000); // allow the server to establish web connection
      isInitialised = true;
    }
  }

  @AfterEach
  void cleanup() {
    closeQuietly(modemSideSession);
    closeQuietly(enterpriseSideSession);
    modemSideSession = null;
    enterpriseSideSession = null;
  }

  @Test
  void dropSemanticsOnlyLastEventPerTopicIsObservedEnterpriseToModem() throws Exception {
    TestReceiver enterpriseReceiver = new TestReceiver();
    TestReceiver modemReceiver = new TestReceiver();

    modemSideSession = createModemSideSession(modemReceiver.listener());
    enterpriseSideSession = createEnterpriseSideSession(enterpriseReceiver.listener());

    String enterprisePublishTopic = enterpriseToModemSatelliteTopic(1);
    String modemReceiveTopic = modemSensorsTopic(1);

    SubscriptionContext subscription = modemReceiver.subscribe(modemSideSession, modemReceiveTopic);

    int publishCount = 25;
    for (int index = 0; index < publishCount; index++) {
      publishText(enterpriseSideSession, enterprisePublishTopic, "msg-" + index);
    }

    boolean received = modemReceiver.await(modemReceiveTopic, WAIT);
    Assertions.assertTrue(received, "Expected at least one replicated event within " + WAIT);

    String lastPayload = modemReceiver.getLastText(modemReceiveTopic);
    Assertions.assertEquals("msg-" + (publishCount - 1), lastPayload, "Only the last event should be observed");

    modemSideSession.removeSubscription(subscription.getKey());
  }

  @Test
  void tenTopicsProduceTenEventsOnePerTopicEnterpriseToModem() throws Exception {
    TestReceiver enterpriseReceiver = new TestReceiver();
    TestReceiver modemReceiver = new TestReceiver();

    modemSideSession = createModemSideSession(modemReceiver.listener());
    enterpriseSideSession = createEnterpriseSideSession(enterpriseReceiver.listener());

    int topicCount = 10;
    SubscriptionContext[] subscriptions = new SubscriptionContext[topicCount];

    for (int topicIndex = 0; topicIndex < topicCount; topicIndex++) {
      subscriptions[topicIndex] = modemReceiver.subscribe(modemSideSession, modemSensorsTopic(topicIndex));
    }

    for (int topicIndex = 0; topicIndex < topicCount; topicIndex++) {
      String enterprisePublishTopic = enterpriseToModemSatelliteTopic(topicIndex);

      for (int messageIndex = 0; messageIndex < 7; messageIndex++) {
        publishText(enterpriseSideSession, enterprisePublishTopic, "t" + topicIndex + "-m" + messageIndex);
      }
    }

    boolean allReceived = modemReceiver.awaitAll(topicCount, WAIT);
    Assertions.assertTrue(allReceived, "Expected 1 event per topic (" + topicCount + ") within " + WAIT);

    for (int topicIndex = 0; topicIndex < topicCount; topicIndex++) {
      String topic = modemSensorsTopic(topicIndex);
      String lastPayload = modemReceiver.getLastText(topic);
      Assertions.assertEquals("t" + topicIndex + "-m6", lastPayload, "Last-per-topic must hold for " + topic);
    }

    for (SubscriptionContext subscription : subscriptions) {
      modemSideSession.removeSubscription(subscription.getKey());
    }
  }

  @Test
  void rawNanoIoTPacketsAreRoutedUsingSinMinEnterpriseToModem() throws Exception {
    TestReceiver enterpriseReceiver = new TestReceiver();
    TestReceiver modemReceiver = new TestReceiver();

    modemSideSession = createModemSideSession(modemReceiver.listener());
    enterpriseSideSession = createEnterpriseSideSession(enterpriseReceiver.listener());

    int sin = 20;
    int min = 7;

    String enterprisePublishTopic = enterpriseToModemRawPublishTopic(sin, min);
    String modemReceiveTopic = modemRawReceiveTopic(sin, min);

    SubscriptionContext subscription = modemReceiver.subscribe(modemSideSession, modemReceiveTopic);

    byte[] raw = buildNanoIoTRawPacket(sin, min, "hello-nano");
    publishRaw(enterpriseSideSession, enterprisePublishTopic, raw);

    boolean received = modemReceiver.await(modemReceiveTopic, WAIT);
    Assertions.assertTrue(received, "Expected raw packet arrival within " + WAIT);

    byte[] observed = modemReceiver.getLastBytes(modemReceiveTopic);
    Assertions.assertNotNull(observed, "Expected payload bytes");
    Assertions.assertTrue(observed.length >= 2, "Raw payload must be at least 2 bytes");

    String tail = new String(observed, StandardCharsets.UTF_8);
    Assertions.assertEquals("hello-nano", tail, "Raw payload tail must be preserved");

    modemSideSession.removeSubscription(subscription.getKey());
  }

  private Session createModemSideSession(MessageListener listener) throws LoginException, IOException {
    return createSession("SatelliteReverseModemSide" + System.nanoTime(), 60, 60, false, listener);
  }

  private Session createEnterpriseSideSession(MessageListener listener) throws LoginException, IOException {
    return createSession("SatelliteReverseEnterpriseSide" + System.nanoTime(), 60, 60, false, listener);
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
    return MODEM_RECEIVE_TELEMETRY_BASE + "/" + runId + "/t/" + topicIndex;
  }

  private String enterpriseToModemSatelliteTopic(int topicIndex) {
    return ENTERPRISE_TO_MODEM_TELEMETRY_BASE + "/" + runId + "/t/" + topicIndex;
  }

  private String enterpriseToModemRawPublishTopic(int sin, int min) {
    return String.format(ENTERPRISE_TO_MODEM_RAW_PUBLISH_TEMPLATE, sin, min);
  }

  private String modemRawReceiveTopic(int sin, int min) {
    return String.format(MODEM_RAW_RECEIVE_TEMPLATE, sin, min);
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