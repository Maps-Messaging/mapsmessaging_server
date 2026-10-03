/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.mqtt5;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.config.protocol.impl.MqttConfig;
import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointStatus;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Publish5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Subscribe5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Unsubscribe5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.ContentType;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.CorrelationData;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessageExpiryInterval;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessageProperty;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessagePropertyFactory;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.PayloadFormatIndicator;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.ResponseTopic;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.SubscriptionIdentifier;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.TopicAlias;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.UserProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MQTT5ProtocolBranchCoverageTest {

  private static final AtomicInteger FIXTURE_IDS = new AtomicInteger();

  @Test
  void constructorExposesConfiguredDefaults() throws Exception {
    Fixture fixture = fixture(0);

    assertEquals("MQTT", fixture.protocol.getName());
    assertEquals("5.0", fixture.protocol.getVersion());
    assertEquals("waiting", fixture.protocol.getSessionId());
    assertEquals(10, fixture.protocol.getServerReceiveMaximum());
    assertEquals(65535, fixture.protocol.getClientReceiveMaximum());
    assertEquals(0, fixture.protocol.getMinimumKeepAlive());
    assertEquals(10_485_760L, fixture.protocol.getMaxBufferSize());
    assertEquals(0, fixture.protocol.getServerTopicAliasMapping().getMaximum());
    assertNull(fixture.protocol.getAuthenticationContext());
  }

  @Test
  void sessionIdTracksAssignedSession() throws Exception {
    Fixture fixture = fixture(0);
    Session session = mock(Session.class);
    when(session.getName()).thenReturn("client-42");

    fixture.protocol.setSession(session);

    assertEquals("client-42", fixture.protocol.getSessionId());
    verify(fixture.endPoint).completedConnection();
  }

  @ParameterizedTest
  @MethodSource("qosMatrix")
  void effectivePublishQosIsMinimumOfSubscriptionAndMessage(
      QualityOfService subscriptionQos,
      QualityOfService messageQos,
      QualityOfService expectedQos) throws Exception {
    Fixture fixture = fixture(0);
    SubscribedEventManager subscription = subscription(subscriptionQos, false, -1);
    Message message = message(41L, messageQos, false, "payload");

    Publish5 publish = fixture.sendPublish("/sensor/value", subscription, message, mock(Runnable.class), false);

    assertEquals(expectedQos, publish.getQos());
    if (expectedQos.isSendPacketId()) {
      assertTrue(publish.getPacketId() > 0);
    } else {
      assertEquals(0, publish.getPacketId());
    }
  }

  @ParameterizedTest
  @MethodSource("retainCases")
  void retainFlagCombinesReplayAndRetainAsPublished(
      boolean retainedReplay,
      boolean retainAsPublished,
      boolean messageRetain,
      boolean expected) throws Exception {
    Fixture fixture = fixture(0);
    SubscribedEventManager subscription = subscription(
        QualityOfService.AT_MOST_ONCE, retainAsPublished, -1);
    Message message = message(51L, QualityOfService.AT_MOST_ONCE, messageRetain, "payload");

    Publish5 publish = fixture.sendPublish(
        "/retained/value", subscription, message, mock(Runnable.class), retainedReplay);

    assertEquals(expected, publish.isRetain());
  }

  @ParameterizedTest
  @MethodSource("packetIdCases")
  void packetIdentifierOnlyAllocatedForQosThatRequiresIt(
      QualityOfService qos,
      boolean expectedPacketId) throws Exception {
    Fixture fixture = fixture(0);
    SubscribedEventManager subscription = subscription(qos, false, -1);
    Message message = message(61L, qos, false, "payload");

    int packetId = fixture.getPacketId(qos, subscription, message);

    assertEquals(expectedPacketId, packetId > 0);
  }

  @ParameterizedTest
  @MethodSource("callbackCases")
  void qosDowngradeControlsCompletionAndAckBehaviour(
      QualityOfService subscriptionQos,
      QualityOfService messageQos,
      boolean expectAck) throws Exception {
    Fixture fixture = fixture(0);
    SubscribedEventManager subscription = subscription(subscriptionQos, false, -1);
    Message message = message(71L, messageQos, false, "payload");
    Runnable completion = mock(Runnable.class);

    Publish5 publish = fixture.sendPublish(
        "/callback/value", subscription, message, completion, false);
    publish.getCallback().run();

    verify(completion).run();
    if (expectAck) {
      verify(subscription).ackReceived(71L);
    } else {
      verify(subscription, never()).ackReceived(anyLong());
    }
  }

  @Test
  void topicAliasIsCreatedReusedAndStopsAtConfiguredMaximum() throws Exception {
    Fixture fixture = fixture(2);
    SubscribedEventManager subscription =
        subscription(QualityOfService.AT_MOST_ONCE, false, -1);

    Publish5 first = fixture.sendPublish(
        "/alpha", subscription,
        message(1, QualityOfService.AT_MOST_ONCE, false, "one"),
        mock(Runnable.class), false);
    Publish5 repeated = fixture.sendPublish(
        "/alpha", subscription,
        message(2, QualityOfService.AT_MOST_ONCE, false, "two"),
        mock(Runnable.class), false);
    Publish5 second = fixture.sendPublish(
        "/beta", subscription,
        message(3, QualityOfService.AT_MOST_ONCE, false, "three"),
        mock(Runnable.class), false);
    Publish5 overLimit = fixture.sendPublish(
        "/gamma", subscription,
        message(4, QualityOfService.AT_MOST_ONCE, false, "four"),
        mock(Runnable.class), false);

    assertEquals("/alpha", first.getDestinationName());
    assertEquals(1, topicAlias(first).getTopicAlias());

    assertEquals("", repeated.getDestinationName());
    assertEquals(1, topicAlias(repeated).getTopicAlias());

    assertEquals("/beta", second.getDestinationName());
    assertEquals(2, topicAlias(second).getTopicAlias());

    assertEquals("/gamma", overLimit.getDestinationName());
    assertNull(topicAlias(overLimit));
  }

  @ParameterizedTest
  @MethodSource("userPropertyCases")
  void dataMapEntriesBecomeUserProperties(Object value, String expected) throws Exception {
    Fixture fixture = fixture(0);
    Message message = basePropertyMessage();
    when(message.getDataMap()).thenReturn(Map.of("key", new TypedData(value)));
    Publish5 publish = emptyPublish();
    SubscribedEventManager subscription =
        subscription(QualityOfService.AT_MOST_ONCE, false, -1);

    fixture.addProperties(message, publish, subscription);

    UserProperty property = (UserProperty) firstProperty(
        publish, MessagePropertyFactory.USER_PROPERTY);
    assertNotNull(property);
    assertEquals("key", property.getUserPropertyName());
    assertEquals(expected, property.getUserPropertyValue());
  }

  @ParameterizedTest
  @MethodSource("presenceCases")
  void contentTypePropertyFollowsMessagePresence(boolean present) throws Exception {
    Fixture fixture = fixture(0);
    Message message = basePropertyMessage();
    when(message.getContentType()).thenReturn(present ? "application/json" : null);
    Publish5 publish = emptyPublish();

    fixture.addProperties(message, publish,
        subscription(QualityOfService.AT_MOST_ONCE, false, -1));

    ContentType property = (ContentType) firstProperty(
        publish, MessagePropertyFactory.CONTENT_TYPE);
    assertEquals(present, property != null);
    if (present) {
      assertEquals("application/json", property.getContentType());
    }
  }

  @ParameterizedTest
  @MethodSource("presenceCases")
  void correlationDataPropertyFollowsMessagePresence(boolean present) throws Exception {
    Fixture fixture = fixture(0);
    Message message = basePropertyMessage();
    byte[] correlation = present ? new byte[]{1, 2, 3} : null;
    when(message.getCorrelationData()).thenReturn(correlation);
    Publish5 publish = emptyPublish();

    fixture.addProperties(message, publish,
        subscription(QualityOfService.AT_MOST_ONCE, false, -1));

    CorrelationData property = (CorrelationData) firstProperty(
        publish, MessagePropertyFactory.CORRELATION_DATA);
    assertEquals(present, property != null);
    if (present) {
      assertArrayEquals(correlation, property.getCorrelationData());
    }
  }

  @ParameterizedTest
  @MethodSource("expiryCases")
  void expiryPropertyOnlyAddedForFutureExpiry(long offsetMillis, boolean expected) throws Exception {
    Fixture fixture = fixture(0);
    Message message = basePropertyMessage();
    long expiry = offsetMillis > 0 ? System.currentTimeMillis() + offsetMillis : offsetMillis;
    when(message.getExpiry()).thenReturn(expiry);
    Publish5 publish = emptyPublish();

    fixture.addProperties(message, publish,
        subscription(QualityOfService.AT_MOST_ONCE, false, -1));

    MessageExpiryInterval property = (MessageExpiryInterval) firstProperty(
        publish, MessagePropertyFactory.MESSAGE_EXPIRY_INTERVAL);
    assertEquals(expected, property != null);
    if (expected) {
      assertTrue(property.getMessageExpiryInterval() > 0);
    }
  }

  @ParameterizedTest
  @MethodSource("presenceCases")
  void payloadFormatIndicatorOnlyAddedForUtf8Messages(boolean utf8) throws Exception {
    Fixture fixture = fixture(0);
    Message message = basePropertyMessage();
    when(message.isUTF8()).thenReturn(utf8);
    Publish5 publish = emptyPublish();

    fixture.addProperties(message, publish,
        subscription(QualityOfService.AT_MOST_ONCE, false, -1));

    PayloadFormatIndicator property = (PayloadFormatIndicator) firstProperty(
        publish, MessagePropertyFactory.PAYLOAD_FORMAT_INDICATOR);
    assertEquals(utf8, property != null);
    if (utf8) {
      assertTrue(property.getPayloadFormatIndicator());
    }
  }

  @ParameterizedTest
  @MethodSource("presenceCases")
  void responseTopicPropertyFollowsMessagePresence(boolean present) throws Exception {
    Fixture fixture = fixture(0);
    Message message = basePropertyMessage();
    when(message.getResponseTopic()).thenReturn(present ? "/reply" : null);
    Publish5 publish = emptyPublish();

    fixture.addProperties(message, publish,
        subscription(QualityOfService.AT_MOST_ONCE, false, -1));

    ResponseTopic property = (ResponseTopic) firstProperty(
        publish, MessagePropertyFactory.RESPONSE_TOPIC);
    assertEquals(present, property != null);
    if (present) {
      assertEquals("/reply", property.getResponseTopicString());
    }
  }

  @ParameterizedTest
  @MethodSource("subscriptionIdentifierCases")
  void subscriptionIdentifierFiltersUnsetAndZeroValues(long id, boolean expected) throws Exception {
    Fixture fixture = fixture(0);
    Message message = basePropertyMessage();
    Publish5 publish = emptyPublish();
    SubscribedEventManager subscription =
        subscription(QualityOfService.AT_MOST_ONCE, false, id);

    fixture.addProperties(message, publish, subscription);

    SubscriptionIdentifier property = (SubscriptionIdentifier) firstProperty(
        publish, MessagePropertyFactory.SUBSCRIPTION_IDENTIFIER);
    assertEquals(expected, property != null);
    if (expected) {
      assertEquals(id, property.getSubscriptionIdentifier());
    }
  }

  @Test
  void multiplePositiveSubscriptionIdentifiersArePreserved() throws Exception {
    Fixture fixture = fixture(0);
    Message message = basePropertyMessage();
    Publish5 publish = emptyPublish();
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    SubscriptionContext first = context(QualityOfService.AT_MOST_ONCE, false, 11);
    SubscriptionContext second = context(QualityOfService.AT_MOST_ONCE, false, 22);
    when(subscription.getContext()).thenReturn(first);
    when(subscription.getContexts()).thenReturn(List.of(first, second));

    fixture.addProperties(message, publish, subscription);

    List<Long> ids = publish.getProperties().values().stream()
        .filter(SubscriptionIdentifier.class::isInstance)
        .map(SubscriptionIdentifier.class::cast)
        .map(SubscriptionIdentifier::getSubscriptionIdentifier)
        .toList();
    assertEquals(List.of(11L, 22L), ids);
  }

  @ParameterizedTest
  @MethodSource("bufferCases")
  void sendMessageHonoursMaximumBufferSize(
      long maximum,
      int payloadSize,
      boolean expectPublish) throws Exception {
    Fixture fixture = fixture(0);
    fixture.protocol.setMaxBufferSize(maximum);
    SubscribedEventManager subscription =
        subscription(QualityOfService.AT_MOST_ONCE, false, -1);
    Message message = message(81L, QualityOfService.AT_MOST_ONCE, false,
        "x".repeat(payloadSize));
    Runnable completion = mock(Runnable.class);
    MessageEvent event = new MessageEvent("/buffer", subscription, message, completion);

    fixture.protocol.sendMessage(event);

    assertEquals(expectPublish, fixture.frames.size() == 1);
    if (expectPublish) {
      verify(completion, never()).run();
    } else {
      verify(completion).run();
    }
  }

  @Test
  void subscribeRemoteWritesSubscribeAndRecordsTopicMapping() throws Exception {
    Fixture fixture = fixture(0);

    fixture.protocol.subscribeRemote(
        "/remote/source",
        "/local/mapped",
        QualityOfService.AT_LEAST_ONCE,
        null,
        null,
        null,
        Map.of());

    assertEquals("/local/mapped",
        fixture.protocol.getTopicNameMapping().get("/remote/source"));
    Subscribe5 subscribe = assertInstanceOf(Subscribe5.class, fixture.onlyFrame());
    assertTrue(subscribe.getMessageId() > 0);
    assertEquals(1, subscribe.getSubscriptionList().size());
    assertEquals("/remote/source", subscribe.getSubscriptionList().get(0).getTopicName());
    assertEquals(QualityOfService.AT_LEAST_ONCE,
        subscribe.getSubscriptionList().get(0).getQualityOfService());
    verify(fixture.endPoint).completedConnection();
  }

  @Test
  void unsubscribeRemoteWritesPacketWithRequestedResource() throws Exception {
    Fixture fixture = fixture(0);

    fixture.protocol.unsubscribeRemote("/remote/source");

    Unsubscribe5 unsubscribe = assertInstanceOf(Unsubscribe5.class, fixture.onlyFrame());
    assertTrue(unsubscribe.getMessageId() > 0);
    assertEquals(List.of("/remote/source"), unsubscribe.getUnsubscribeList());
  }

  @ParameterizedTest
  @MethodSource("localSelectorCases")
  void subscribeLocalBuildsExpectedSubscriptionContext(
      String selector,
      boolean expectedSelector) throws Exception {
    Fixture fixture = fixture(0);
    Session session = mock(Session.class);
    fixture.protocol.setSession(session);

    fixture.protocol.subscribeLocal(
        "/local/source",
        "/remote/mapped",
        QualityOfService.EXACTLY_ONCE,
        selector,
        null,
        null,
        null,
        Map.of());

    var captor = org.mockito.ArgumentCaptor.forClass(SubscriptionContext.class);
    verify(session).addSubscription(captor.capture());
    SubscriptionContext context = captor.getValue();
    assertEquals("/local/source", context.getDestinationName());
    assertEquals(QualityOfService.EXACTLY_ONCE, context.getQualityOfService());
    assertEquals(1024, context.getReceiveMaximum());
    assertTrue(context.isAllowOverlap());
    assertEquals(expectedSelector ? selector : null, context.getSelector());
    assertEquals("/remote/mapped",
        fixture.protocol.getTopicNameMapping().get("/local/source"));
  }

  @Test
  void unsubscribeLocalDelegatesToSession() throws Exception {
    Fixture fixture = fixture(0);
    Session session = mock(Session.class);
    fixture.protocol.setSession(session);

    fixture.protocol.unsubscribeLocal("/local/source");

    verify(session).removeSubscription("/local/source");
  }

  private static Fixture fixture(int serverAliasMaximum) throws Exception {
    EndPoint endPoint = mock(EndPoint.class);
    EndPointStatus status = mock(EndPointStatus.class);
    MqttConfig mqttConfig = new MqttConfig();
    mqttConfig.setServerMaximumTopicAlias(serverAliasMaximum);

    EndPointConfigDTO endPointConfig = new EndPointConfigDTO("tcp");
    EndPointServerConfigDTO serverConfig = new EndPointServerConfigDTO();
    serverConfig.setName("mqtt5-coverage");
    serverConfig.setEndPointConfig(endPointConfig);
    serverConfig.setProtocolConfigs(List.of(mqttConfig));

    int id = FIXTURE_IDS.incrementAndGet();
    when(endPoint.getConfig()).thenReturn(serverConfig);
    when(endPoint.getName()).thenReturn("mqtt5-coverage-" + id);
    when(endPoint.getJMXTypePath()).thenReturn(List.of("Coverage=" + id));
    when(endPoint.getEndPointStatus()).thenReturn(status);

    MQTT5Protocol protocol = spy(new MQTT5Protocol(endPoint));
    List<ServerPacket> frames = new ArrayList<>();
    doAnswer(invocation -> {
      frames.add(invocation.getArgument(0));
      return null;
    }).when(protocol).writeFrame(any(ServerPacket.class));

    return new Fixture(protocol, endPoint, frames);
  }

  private static SubscribedEventManager subscription(
      QualityOfService qos,
      boolean retainAsPublished,
      long subscriptionId) {
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    SubscriptionContext context = context(qos, retainAsPublished, subscriptionId);
    when(subscription.getContext()).thenReturn(context);
    when(subscription.getContexts()).thenReturn(List.of(context));
    return subscription;
  }

  private static SubscriptionContext context(
      QualityOfService qos,
      boolean retainAsPublished,
      long subscriptionId) {
    SubscriptionContext context = new SubscriptionContext("/source");
    context.setQualityOfService(qos);
    context.setRetainAsPublish(retainAsPublished);
    context.setSubscriptionId(subscriptionId);
    return context;
  }

  private static Message message(
      long identifier,
      QualityOfService qos,
      boolean retain,
      String payload) {
    Message message = basePropertyMessage();
    when(message.getIdentifier()).thenReturn(identifier);
    when(message.getQualityOfService()).thenReturn(qos);
    when(message.isRetain()).thenReturn(retain);
    when(message.getOpaqueData()).thenReturn(payload.getBytes(StandardCharsets.UTF_8));
    return message;
  }

  private static Message basePropertyMessage() {
    Message message = mock(Message.class);
    when(message.getOpaqueData()).thenReturn(new byte[0]);
    when(message.getQualityOfService()).thenReturn(QualityOfService.AT_MOST_ONCE);
    when(message.getDataMap()).thenReturn(Map.of());
    when(message.getExpiry()).thenReturn(0L);
    return message;
  }

  private static Publish5 emptyPublish() {
    return new Publish5(
        new byte[0],
        QualityOfService.AT_MOST_ONCE,
        0,
        "/topic",
        false);
  }

  private static MessageProperty firstProperty(Publish5 publish, int id) {
    Collection<MessageProperty> properties = publish.getProperties().values();
    return properties.stream().filter(property -> property.getId() == id).findFirst().orElse(null);
  }

  private static TopicAlias topicAlias(Publish5 publish) {
    return (TopicAlias) firstProperty(publish, MessagePropertyFactory.TOPIC_ALIAS);
  }

  private static Stream<Arguments> qosMatrix() {
    List<Arguments> result = new ArrayList<>();
    for (QualityOfService subscription :
        List.of(
            QualityOfService.AT_MOST_ONCE,
            QualityOfService.AT_LEAST_ONCE,
            QualityOfService.EXACTLY_ONCE)) {
      for (QualityOfService message :
          List.of(
              QualityOfService.AT_MOST_ONCE,
              QualityOfService.AT_LEAST_ONCE,
              QualityOfService.EXACTLY_ONCE)) {
        result.add(Arguments.of(
            subscription,
            message,
            QualityOfService.getInstance(Math.min(subscription.getLevel(), message.getLevel()))));
      }
    }
    return result.stream();
  }

  private static Stream<Arguments> retainCases() {
    List<Arguments> result = new ArrayList<>();
    for (boolean replay : List.of(false, true)) {
      for (boolean rap : List.of(false, true)) {
        for (boolean retained : List.of(false, true)) {
          result.add(Arguments.of(replay, rap, retained, replay || (rap && retained)));
        }
      }
    }
    return result.stream();
  }

  private static Stream<Arguments> packetIdCases() {
    return Stream.of(
        Arguments.of(QualityOfService.AT_MOST_ONCE, false),
        Arguments.of(QualityOfService.AT_LEAST_ONCE, true),
        Arguments.of(QualityOfService.EXACTLY_ONCE, true)
    );
  }

  private static Stream<Arguments> callbackCases() {
    return Stream.of(
        Arguments.of(
            QualityOfService.AT_LEAST_ONCE,
            QualityOfService.AT_MOST_ONCE,
            true),
        Arguments.of(
            QualityOfService.EXACTLY_ONCE,
            QualityOfService.AT_MOST_ONCE,
            true),
        Arguments.of(
            QualityOfService.AT_LEAST_ONCE,
            QualityOfService.AT_LEAST_ONCE,
            false),
        Arguments.of(
            QualityOfService.EXACTLY_ONCE,
            QualityOfService.EXACTLY_ONCE,
            false)
    );
  }

  private static Stream<Arguments> userPropertyCases() {
    return Stream.of(
        Arguments.of("value", "value"),
        Arguments.of(42, "42"),
        Arguments.of(true, "true"),
        Arguments.of(12.5d, "12.5")
    );
  }

  private static Stream<Arguments> presenceCases() {
    return Stream.of(Arguments.of(false), Arguments.of(true));
  }

  private static Stream<Arguments> expiryCases() {
    return Stream.of(
        Arguments.of(-1L, false),
        Arguments.of(0L, false),
        Arguments.of(120_000L, true)
    );
  }

  private static Stream<Arguments> subscriptionIdentifierCases() {
    return Stream.of(
        Arguments.of(-1L, false),
        Arguments.of(0L, false),
        Arguments.of(1L, true),
        Arguments.of(127L, true),
        Arguments.of(128L, true)
    );
  }

  private static Stream<Arguments> bufferCases() {
    return Stream.of(
        Arguments.of(0L, 64, true),
        Arguments.of(65L, 64, true),
        Arguments.of(64L, 64, false),
        Arguments.of(63L, 64, false)
    );
  }

  private static Stream<Arguments> localSelectorCases() {
    return Stream.of(
        Arguments.of(null, false),
        Arguments.of("", false),
        Arguments.of("priority > 4", true)
    );
  }

  private static final class Fixture {
    private final MQTT5Protocol protocol;
    private final EndPoint endPoint;
    private final List<ServerPacket> frames;

    private Fixture(
        MQTT5Protocol protocol,
        EndPoint endPoint,
        List<ServerPacket> frames) {
      this.protocol = protocol;
      this.endPoint = endPoint;
      this.frames = frames;
    }

    private Publish5 sendPublish(
        String topic,
        SubscribedEventManager subscription,
        Message message,
        Runnable completion,
        boolean retainedReplay) throws Exception {
      Method method = MQTT5Protocol.class.getDeclaredMethod(
          "sendPublishFrame",
          String.class,
          SubscribedEventManager.class,
          Message.class,
          Runnable.class,
          boolean.class);
      method.setAccessible(true);
      int before = frames.size();
      method.invoke(protocol, topic, subscription, message, completion, retainedReplay);
      assertEquals(before + 1, frames.size());
      return assertInstanceOf(Publish5.class, frames.get(frames.size() - 1));
    }

    private int getPacketId(
        QualityOfService qos,
        SubscribedEventManager subscription,
        Message message) throws Exception {
      Method method = MQTT5Protocol.class.getDeclaredMethod(
          "getPacketId",
          QualityOfService.class,
          SubscribedEventManager.class,
          Message.class);
      method.setAccessible(true);
      return (int) method.invoke(protocol, qos, subscription, message);
    }

    private void addProperties(
        Message message,
        Publish5 publish,
        SubscribedEventManager subscription) throws Exception {
      Method method = MQTT5Protocol.class.getDeclaredMethod(
          "addProperties",
          Message.class,
          Publish5.class,
          SubscribedEventManager.class);
      method.setAccessible(true);
      method.invoke(protocol, message, publish, subscription);
    }

    private ServerPacket onlyFrame() {
      assertEquals(1, frames.size());
      return frames.get(0);
    }
  }
}
