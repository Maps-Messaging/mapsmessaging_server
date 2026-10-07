package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.config.protocol.impl.MqttSnConfig;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.*;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.TopicAliasManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("conformance")
@Tag("conformance-core")
@Tag("mqtt-sn")
class MqttSn12ListenerSpecificationTest {

  private static final String SPEC = "MQTT-SN Version 1.2";
  private static final String SOURCE = ProtocolRequirement.MQTT_SN_12_SOURCE;

  @ParameterizedTest
  @MethodSource("shortNames")
  @ProtocolRequirement(specification = SPEC, value = "Section 5.4.15 SUBSCRIBE: short topic name is the literal two-octet topic selector", source = SOURCE)
  void subscribeShortTopicCreatesSubscriptionForLiteralTwoByteName(String shortName)
      throws Exception {
    Fixture fixture = fixture();
    Subscribe subscribe = subscribeShort(shortName, 0x1234);

    MQTT_SNPacket response = new SubscribeListener().handlePacket(
        subscribe,
        fixture.session,
        fixture.endPoint,
        fixture.protocol,
        fixture.engine);

    SubAck subAck = assertInstanceOf(SubAck.class, response);
    assertEquals(ReasonCodes.SUCCESS, subAck.getStatus());
    assertEquals(0x1234, subAck.getMsgId());

    ArgumentCaptor<SubscriptionContext> context =
        ArgumentCaptor.forClass(SubscriptionContext.class);
    verify(fixture.session).addSubscription(context.capture());
    assertEquals(shortName, context.getValue().getFilter());
  }

  @ParameterizedTest
  @MethodSource("normalTopics")
  @ProtocolRequirement(specification = SPEC, value = "Sections 5.4.17 and 6.9 UNSUBSCRIBE: normal topic-name form identifies the subscription to remove", source = SOURCE)
  void unsubscribeNormalTopicRemovesLiteralTopicName(String topic) throws Exception {
    Fixture fixture = fixture();
    Unsubscribe unsubscribe = unsubscribeNormal(topic, 0x1111);

    MQTT_SNPacket response = new UnsubscribeListener().handlePacket(
        unsubscribe,
        fixture.session,
        fixture.endPoint,
        fixture.protocol,
        fixture.engine);

    UnSubAck ack = assertInstanceOf(UnSubAck.class, response);
    assertEquals(0x1111, ack.getMsgId());
    verify(fixture.session).removeSubscription(topic);
    verify(fixture.engine).removeSubscribeResponse(topic);
  }

  @ParameterizedTest
  @MethodSource("shortNames")
  @ProtocolRequirement(specification = SPEC, value = "Sections 5.4.17 and 6.9 UNSUBSCRIBE: short topic-name form identifies the subscription to remove", source = SOURCE)
  void unsubscribeShortTopicRemovesLiteralTwoByteTopicName(String shortName)
      throws Exception {
    Fixture fixture = fixture();
    Unsubscribe unsubscribe = unsubscribeShort(shortName, 0x2222);

    MQTT_SNPacket response = new UnsubscribeListener().handlePacket(
        unsubscribe,
        fixture.session,
        fixture.endPoint,
        fixture.protocol,
        fixture.engine);

    assertInstanceOf(UnSubAck.class, response);
    verify(fixture.session).removeSubscription(shortName);
    verify(fixture.engine).removeSubscribeResponse(shortName);
  }

  @ParameterizedTest
  @MethodSource("predefinedTopics")
  @ProtocolRequirement(specification = SPEC, value = "Sections 5.3.4 and 5.4.17 UNSUBSCRIBE: predefined topic id form selects a predefined topic", source = SOURCE)
  void unsubscribePredefinedTopicResolvesByAddressAndTopicType(
      int topicId, String resolvedTopic) throws Exception {
    Fixture fixture = fixture();
    Unsubscribe unsubscribe = unsubscribePredefined(topicId, 0x3333);
    when(fixture.aliasManager.getTopic(
        fixture.address,
        topicId,
        MQTT_SNPacket.TOPIC_PRE_DEFINED_ID))
        .thenReturn(resolvedTopic);

    MQTT_SNPacket response = new UnsubscribeListener().handlePacket(
        unsubscribe,
        fixture.session,
        fixture.endPoint,
        fixture.protocol,
        fixture.engine);

    assertInstanceOf(UnSubAck.class, response);
    verify(fixture.session).removeSubscription(resolvedTopic);
    verify(fixture.engine).removeSubscribeResponse(resolvedTopic);
  }

  @ParameterizedTest
  @MethodSource("predefinedTopics")
  @ProtocolRequirement(specification = SPEC, value = "Sections 5.3.4 and 5.4.15 SUBSCRIBE: predefined topic id form selects a predefined topic", source = SOURCE)
  void subscribePredefinedTopicUsesConfiguredTopicMapping(
      int topicId, String resolvedTopic) throws Exception {
    Fixture fixture = fixture();
    Subscribe subscribe = subscribePredefined(topicId, 0x4444);
    when(fixture.aliasManager.getTopic(
        fixture.address,
        topicId,
        MQTT_SNPacket.TOPIC_PRE_DEFINED_ID))
        .thenReturn(resolvedTopic);

    MQTT_SNPacket response = new SubscribeListener().handlePacket(
        subscribe,
        fixture.session,
        fixture.endPoint,
        fixture.protocol,
        fixture.engine);

    SubAck ack = assertInstanceOf(SubAck.class, response);
    assertEquals(ReasonCodes.SUCCESS, ack.getStatus());
    assertEquals(topicId, ack.getTopicId());

    ArgumentCaptor<SubscriptionContext> context =
        ArgumentCaptor.forClass(SubscriptionContext.class);
    verify(fixture.session).addSubscription(context.capture());
    assertEquals(resolvedTopic, context.getValue().getFilter());
  }

  @ParameterizedTest
  @MethodSource("normalTopics")
  @ProtocolRequirement(specification = SPEC, value = "Section 5.4.15 SUBSCRIBE: normal topic-name form carries the topic name in the SUBSCRIBE packet", source = SOURCE)
  void subscribeNormalTopicCreatesSubscriptionWithoutRegistrationLookup(String topic)
      throws Exception {
    Fixture fixture = fixture();
    Subscribe subscribe = subscribeNormal(topic, 0x5555);

    MQTT_SNPacket response = new SubscribeListener().handlePacket(
        subscribe,
        fixture.session,
        fixture.endPoint,
        fixture.protocol,
        fixture.engine);

    SubAck ack = assertInstanceOf(SubAck.class, response);
    assertEquals(ReasonCodes.SUCCESS, ack.getStatus());

    ArgumentCaptor<SubscriptionContext> context =
        ArgumentCaptor.forClass(SubscriptionContext.class);
    verify(fixture.session).addSubscription(context.capture());
    if (topic.startsWith(DestinationMode.SCHEMA.getNamespace())) {
      assertEquals(DestinationMode.SCHEMA, context.getValue().getDestinationMode());
      assertEquals(
          topic.substring(DestinationMode.SCHEMA.getNamespace().length()),
          context.getValue().getFilter());
    } else {
      assertEquals(DestinationMode.NORMAL, context.getValue().getDestinationMode());
      assertEquals(topic, context.getValue().getFilter());
    }
  }

  @Test
  @ProtocolRequirement(specification = SPEC, value = "Section 5.4.16 SUBACK: invalid or unsupported topic selection is reported by ReturnCode", source = SOURCE)
  void unresolvedPredefinedSubscribeReturnsInvalidTopicId() throws Exception {
    Fixture fixture = fixture();
    Subscribe subscribe = subscribePredefined(77, 9);

    SubAck response = assertInstanceOf(
        SubAck.class,
        new SubscribeListener().handlePacket(
            subscribe,
            fixture.session,
            fixture.endPoint,
            fixture.protocol,
            fixture.engine));

    assertEquals(ReasonCodes.INVALID_TOPIC_ALIAS, response.getStatus());
    verify(fixture.session, never()).addSubscription(any());
  }

  @Test
  @ProtocolRequirement(specification = SPEC, value = "Sections 5.4.15-5.4.16 SUBSCRIBE/SUBACK: granted QoS is returned in SUBACK", source = SOURCE)
  void subscribePreservesRequestedQosInSubscriptionAndSubAck() throws Exception {
    Fixture fixture = fixture();
    Subscribe subscribe = parseSubscribe(
        bytes(
            9,
            MQTT_SNPacket.SUBSCRIBE,
            0b00100000,
            0,
            3,
            'a',
            '/',
            'b',
            'c'));

    SubAck response = assertInstanceOf(
        SubAck.class,
        new SubscribeListener().handlePacket(
            subscribe,
            fixture.session,
            fixture.endPoint,
            fixture.protocol,
            fixture.engine));

    assertEquals(QualityOfService.AT_LEAST_ONCE, response.getQoS());
    ArgumentCaptor<SubscriptionContext> context =
        ArgumentCaptor.forClass(SubscriptionContext.class);
    verify(fixture.session).addSubscription(context.capture());
    assertEquals(QualityOfService.AT_LEAST_ONCE, context.getValue().getQualityOfService());
  }

  private Fixture fixture() {
    Session session = mock(Session.class);
    EndPoint endPoint = mock(EndPoint.class, RETURNS_DEEP_STUBS);
    Protocol protocol = mock(Protocol.class);
    StateEngine engine = mock(StateEngine.class);
    TopicAliasManager aliasManager = mock(TopicAliasManager.class);
    MqttSnConfig config = mock(MqttSnConfig.class);
    InetSocketAddress address = new InetSocketAddress("127.0.0.1", 1884);

    when(engine.getTopicAliasManager()).thenReturn(aliasManager);
    when(endPoint.getConfig().getProtocolConfig("mqtt-sn")).thenReturn(config);
    when(config.getReceiveMaximum()).thenReturn(20);

    return new Fixture(session, endPoint, protocol, engine, aliasManager, address);
  }

  private Subscribe subscribeNormal(String topic, int msgId) throws Exception {
    byte[] name = topic.getBytes(StandardCharsets.UTF_8);
    byte[] wire = new byte[5 + name.length];
    wire[0] = (byte) wire.length;
    wire[1] = (byte) MQTT_SNPacket.SUBSCRIBE;
    wire[2] = 0;
    wire[3] = (byte) ((msgId >>> 8) & 0xff);
    wire[4] = (byte) (msgId & 0xff);
    System.arraycopy(name, 0, wire, 5, name.length);
    Subscribe subscribe = parseSubscribe(wire);
    subscribe.setFromAddress(new InetSocketAddress("127.0.0.1", 1884));
    return subscribe;
  }

  private Subscribe subscribeShort(String name, int msgId) throws Exception {
    byte[] shortName = name.getBytes(StandardCharsets.US_ASCII);
    Subscribe subscribe = parseSubscribe(
        bytes(
            7,
            MQTT_SNPacket.SUBSCRIBE,
            MQTT_SNPacket.TOPIC_SHORT_NAME,
            (msgId >>> 8) & 0xff,
            msgId & 0xff,
            shortName[0] & 0xff,
            shortName[1] & 0xff));
    subscribe.setFromAddress(new InetSocketAddress("127.0.0.1", 1884));
    return subscribe;
  }

  private Subscribe subscribePredefined(int topicId, int msgId) throws Exception {
    Subscribe subscribe = parseSubscribe(
        bytes(
            7,
            MQTT_SNPacket.SUBSCRIBE,
            MQTT_SNPacket.TOPIC_PRE_DEFINED_ID,
            (msgId >>> 8) & 0xff,
            msgId & 0xff,
            (topicId >>> 8) & 0xff,
            topicId & 0xff));
    subscribe.setFromAddress(new InetSocketAddress("127.0.0.1", 1884));
    return subscribe;
  }

  private Unsubscribe unsubscribeNormal(String topic, int msgId) throws Exception {
    byte[] name = topic.getBytes(StandardCharsets.UTF_8);
    byte[] wire = new byte[5 + name.length];
    wire[0] = (byte) wire.length;
    wire[1] = (byte) MQTT_SNPacket.UNSUBSCRIBE;
    wire[2] = 0;
    wire[3] = (byte) ((msgId >>> 8) & 0xff);
    wire[4] = (byte) (msgId & 0xff);
    System.arraycopy(name, 0, wire, 5, name.length);
    Unsubscribe unsubscribe = parseUnsubscribe(wire);
    unsubscribe.setFromAddress(new InetSocketAddress("127.0.0.1", 1884));
    return unsubscribe;
  }

  private Unsubscribe unsubscribeShort(String name, int msgId) throws Exception {
    byte[] shortName = name.getBytes(StandardCharsets.US_ASCII);
    Unsubscribe unsubscribe = parseUnsubscribe(
        bytes(
            7,
            MQTT_SNPacket.UNSUBSCRIBE,
            MQTT_SNPacket.TOPIC_SHORT_NAME,
            (msgId >>> 8) & 0xff,
            msgId & 0xff,
            shortName[0] & 0xff,
            shortName[1] & 0xff));
    unsubscribe.setFromAddress(new InetSocketAddress("127.0.0.1", 1884));
    return unsubscribe;
  }

  private Unsubscribe unsubscribePredefined(int topicId, int msgId) throws Exception {
    Unsubscribe unsubscribe = parseUnsubscribe(
        bytes(
            7,
            MQTT_SNPacket.UNSUBSCRIBE,
            MQTT_SNPacket.TOPIC_PRE_DEFINED_ID,
            (msgId >>> 8) & 0xff,
            msgId & 0xff,
            (topicId >>> 8) & 0xff,
            topicId & 0xff));
    unsubscribe.setFromAddress(new InetSocketAddress("127.0.0.1", 1884));
    return unsubscribe;
  }

  private Subscribe parseSubscribe(byte[] wire) throws Exception {
    return assertInstanceOf(
        Subscribe.class,
        new PacketFactory().parseFrame(new Packet(ByteBuffer.wrap(wire))));
  }

  private Unsubscribe parseUnsubscribe(byte[] wire) throws Exception {
    return assertInstanceOf(
        Unsubscribe.class,
        new PacketFactory().parseFrame(new Packet(ByteBuffer.wrap(wire))));
  }

  private static byte[] bytes(int... values) {
    byte[] result = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      result[i] = (byte) values[i];
    }
    return result;
  }

  private static Stream<Arguments> shortNames() {
    return Stream.of(
        Arguments.of("AB"),
        Arguments.of("xy"),
        Arguments.of("01"),
        Arguments.of("/a"),
        Arguments.of("Z_"),
        Arguments.of("++"),
        Arguments.of("##"),
        Arguments.of("a."));
  }

  private static Stream<Arguments> normalTopics() {
    return Stream.of(
        Arguments.of("a"),
        Arguments.of("sensor/temp"),
        Arguments.of("/root/topic"),
        Arguments.of("a/b/c"),
        Arguments.of("factory/line/temperature"),
        Arguments.of("+"),
        Arguments.of("#"),
        Arguments.of("$schema/test"),
        Arguments.of("topic with spaces"),
        Arguments.of("12345678901234567890"));
  }

  private static Stream<Arguments> predefinedTopics() {
    return Stream.of(
        Arguments.of(1, "pre/1"),
        Arguments.of(2, "pre/2"),
        Arguments.of(42, "sensor/predefined"),
        Arguments.of(255, "pre/255"),
        Arguments.of(1024, "pre/1024"));
  }

  private record Fixture(
      Session session,
      EndPoint endPoint,
      Protocol protocol,
      StateEngine engine,
      TopicAliasManager aliasManager,
      InetSocketAddress address) {
  }
}
