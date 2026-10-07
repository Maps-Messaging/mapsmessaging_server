package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet;

import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Tag("conformance")
@Tag("conformance-core")
@Tag("mqtt-sn")
class MqttSn12SpecificationComplianceTest {

  private static final String SPEC = "MQTT-SN Version 1.2";
  private static final String SOURCE = ProtocolRequirement.MQTT_SN_12_SOURCE;

  private final PacketFactory factory = new PacketFactory();

  @Test
  @ProtocolRequirement(specification = SPEC, value = "Section 5.4.4 CONNECT: ProtocolId MUST be 0x01 for MQTT-SN 1.2", source = SOURCE)
  void connectProtocolIdOneIsAccepted() throws Exception {
    MQTT_SNPacket packet = parse(bytes(8, MQTT_SNPacket.CONNECT, 0x04, 0x01, 0, 30, 'i', 'd'));

    Connect connect = assertInstanceOf(Connect.class, packet);
    assertEquals(1, connect.getProtocolId());
    assertEquals(30, connect.getDuration());
    assertEquals("id", connect.getClientId());
    assertTrue(connect.clean());
  }

  @ParameterizedTest
  @ValueSource(ints = {0x00, 0x02, 0x7f, 0xff})
  @ProtocolRequirement(specification = SPEC, value = "Section 5.4.4 CONNECT: ProtocolId MUST be 0x01 for MQTT-SN 1.2", source = SOURCE)
  void mqttSn12RejectsAnyConnectProtocolIdOtherThanOne(int protocolId) throws Exception {
    MQTT_SNPacket packet =
        parse(bytes(8, MQTT_SNPacket.CONNECT, 0x04, protocolId, 0, 30, 'i', 'd'));

    ConnAck connAck = assertInstanceOf(
        ConnAck.class,
        packet,
        "MQTT-SN 1.2 CONNECT ProtocolId is exactly 0x01");
    assertEquals(ReasonCodes.NOT_SUPPORTED, connAck.getStatus());
  }

  @ParameterizedTest
  @MethodSource("qosFlags")
  @ProtocolRequirement(specification = SPEC, value = "Sections 5.3.4 and 5.4.12 PUBLISH: QoS bits encode levels 0, 1, 2 and -1", source = SOURCE)
  void publishDecodesAllMqttSn12QosBitPatterns(
      int qosBits, QualityOfService expected) throws Exception {
    Publish publish = assertInstanceOf(
        Publish.class,
        parse(bytes(7, MQTT_SNPacket.PUBLISH, qosBits << 5, 0, 7, 0, 0)));

    assertEquals(expected, publish.getQoS());
  }

  @Test
  @ProtocolRequirement(specification = SPEC, value = "Section 6.8 QoS -1: PUBLISH uses predefined or short topic and MsgId 0 without a connection", source = SOURCE)
  void qosMinusOnePublishUsesPredefinedTopicIdAndZeroMessageId() throws Exception {
    Publish publish = assertInstanceOf(
        Publish.class,
        parse(bytes(
            9,
            MQTT_SNPacket.PUBLISH,
            0b01100001,
            0,
            42,
            0,
            0,
            'x',
            'y')));

    assertEquals(QualityOfService.MQTT_SN_REGISTERED, publish.getQoS());
    assertEquals(MQTT_SNPacket.TOPIC_PRE_DEFINED_ID, publish.getTopicIdType());
    assertEquals(42, publish.getTopicId());
    assertEquals(0, publish.getMessageId());
  }

  @Test
  @ProtocolRequirement(specification = SPEC, value = "Sections 5.3.4 and 6.8 QoS -1: TopicIdType 0b10 carries a two-octet short topic name", source = SOURCE)
  void qosMinusOnePublishShortTopicCarriesTwoOctetTopicName() throws Exception {
    Publish publish = assertInstanceOf(
        Publish.class,
        parse(bytes(
            9,
            MQTT_SNPacket.PUBLISH,
            0b01100010,
            'A',
            'B',
            0,
            0,
            'x',
            'y')));

    assertEquals(QualityOfService.MQTT_SN_REGISTERED, publish.getQoS());
    assertEquals(MQTT_SNPacket.TOPIC_SHORT_NAME, publish.getTopicIdType());
    byte[] encoded = new byte[]{
        (byte) ((publish.getTopicId() >>> 8) & 0xff),
        (byte) (publish.getTopicId() & 0xff)
    };
    assertEquals(
        "AB",
        new String(encoded, StandardCharsets.UTF_8),
        "TopicIdType=0b10 is a two-octet short topic name, not a registered/predefined ID");
  }

  @ParameterizedTest
  @MethodSource("shortNames")
  @ProtocolRequirement(specification = SPEC, value = "Section 5.4.15 SUBSCRIBE: TopicIdType 0b10 identifies a two-octet short topic name", source = SOURCE)
  void subscribeShortTopicIsExposedAsTwoOctetTopicName(String shortName) throws Exception {
    byte[] name = shortName.getBytes(StandardCharsets.US_ASCII);
    Subscribe subscribe = assertInstanceOf(
        Subscribe.class,
        parse(bytes(
            7,
            MQTT_SNPacket.SUBSCRIBE,
            0b00000010,
            0x12,
            0x34,
            name[0] & 0xff,
            name[1] & 0xff)));

    assertEquals(MQTT_SNPacket.TOPIC_SHORT_NAME, subscribe.getTopicIdType());
    assertEquals(
        shortName,
        subscribe.getTopicName(),
        "MQTT-SN 1.2 short topic names are the two bytes carried after MsgId");
  }

  @ParameterizedTest
  @MethodSource("shortNames")
  @ProtocolRequirement(specification = SPEC, value = "Section 5.4.17 UNSUBSCRIBE: TopicIdType 0b10 identifies a two-octet short topic name", source = SOURCE)
  void unsubscribeShortTopicIsExposedAsTwoOctetTopicName(String shortName) throws Exception {
    byte[] name = shortName.getBytes(StandardCharsets.US_ASCII);
    Unsubscribe unsubscribe = assertInstanceOf(
        Unsubscribe.class,
        parse(bytes(
            7,
            MQTT_SNPacket.UNSUBSCRIBE,
            0b00000010,
            0x12,
            0x34,
            name[0] & 0xff,
            name[1] & 0xff)));

    assertEquals(MQTT_SNPacket.TOPIC_SHORT_NAME, unsubscribe.topicIdType());
    assertEquals(
        shortName,
        unsubscribe.getTopicName(),
        "MQTT-SN 1.2 short-topic UNSUBSCRIBE carries the literal two-octet name");
  }

  @ParameterizedTest
  @MethodSource("reservedTopicTypePackets")
  @ProtocolRequirement(specification = SPEC, value = "Section 5.3.4 Flags: TopicIdType 0b11 is reserved in MQTT-SN 1.2", source = SOURCE)
  void reservedTopicIdTypeIsRejected(byte[] wire) {
    assertThrows(
        IOException.class,
        () -> parse(wire),
        "TopicIdType=0b11 is reserved in MQTT-SN 1.2 and must not be accepted as a valid topic selector");
  }

  @ParameterizedTest
  @MethodSource("normalTopicNames")
  @ProtocolRequirement(specification = SPEC, value = "Section 5.4.15 SUBSCRIBE: TopicIdType 0b00 carries a normal topic name", source = SOURCE)
  void subscribeNormalTopicPreservesCompleteTopicName(String topic) throws Exception {
    byte[] topicBytes = topic.getBytes(StandardCharsets.UTF_8);
    byte[] wire = new byte[5 + topicBytes.length];
    wire[0] = (byte) wire.length;
    wire[1] = (byte) MQTT_SNPacket.SUBSCRIBE;
    wire[2] = 0;
    wire[3] = 0x12;
    wire[4] = 0x34;
    System.arraycopy(topicBytes, 0, wire, 5, topicBytes.length);

    Subscribe subscribe = assertInstanceOf(Subscribe.class, parse(wire));

    assertEquals(MQTT_SNPacket.TOPIC_NAME, subscribe.getTopicIdType());
    assertEquals(topic, subscribe.getTopicName());
    assertEquals(0x1234, subscribe.getMsgId());
  }

  @ParameterizedTest
  @MethodSource("normalTopicNames")
  @ProtocolRequirement(specification = SPEC, value = "Section 5.4.17 UNSUBSCRIBE: TopicIdType 0b00 carries a normal topic name", source = SOURCE)
  void unsubscribeNormalTopicPreservesCompleteTopicName(String topic) throws Exception {
    byte[] topicBytes = topic.getBytes(StandardCharsets.UTF_8);
    byte[] wire = new byte[5 + topicBytes.length];
    wire[0] = (byte) wire.length;
    wire[1] = (byte) MQTT_SNPacket.UNSUBSCRIBE;
    wire[2] = 0;
    wire[3] = 0x45;
    wire[4] = 0x67;
    System.arraycopy(topicBytes, 0, wire, 5, topicBytes.length);

    Unsubscribe unsubscribe = assertInstanceOf(Unsubscribe.class, parse(wire));

    assertEquals(MQTT_SNPacket.TOPIC_NAME, unsubscribe.topicIdType());
    assertEquals(topic, unsubscribe.getTopicName());
    assertEquals(0x4567, unsubscribe.getMsgId());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 30, 60, 300, 65535})
  @ProtocolRequirement(specification = SPEC, value = "Section 5.4.21 DISCONNECT: optional Duration is a two-octet value in seconds", source = SOURCE)
  void disconnectDurationIsUnsignedSixteenBitSeconds(int duration) throws Exception {
    Disconnect disconnect = assertInstanceOf(
        Disconnect.class,
        parse(bytes(
            4,
            MQTT_SNPacket.DISCONNECT,
            (duration >>> 8) & 0xff,
            duration & 0xff)));

    assertEquals(duration, disconnect.getDuration());
  }

  @ParameterizedTest
  @MethodSource("clientIds")
  @ProtocolRequirement(specification = SPEC, value = "Section 5.4.19 PINGREQ: ClientId MAY be included by a sleeping client when waking", source = SOURCE)
  void pingRequestMayCarryClientIdForSleepingClient(String clientId) throws Exception {
    byte[] client = clientId.getBytes(StandardCharsets.UTF_8);
    byte[] wire = new byte[2 + client.length];
    wire[0] = (byte) wire.length;
    wire[1] = (byte) MQTT_SNPacket.PINGREQ;
    System.arraycopy(client, 0, wire, 2, client.length);

    PingRequest ping = assertInstanceOf(PingRequest.class, parse(wire));

    assertEquals(clientId, ping.getClientId());
  }


  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section 5.4.10 REGISTER: TopicId, MsgId and TopicName are carried in the registration request",
      source = SOURCE)
  void registerCarriesTopicIdMessageIdAndName() throws Exception {
    Register register = assertInstanceOf(
        Register.class,
        parse(bytes(14, MQTT_SNPacket.REGISTER, 0x12, 0x34, 0x45, 0x67,
            's', 'e', 'n', 's', 'o', 'r', '/', 'x')));

    assertEquals(0x1234, register.getTopicId());
    assertEquals(0x4567, register.getMessageId());
    assertEquals("sensor/x", register.getTopic());
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section 5.4.11 REGACK: acknowledgment returns TopicId, MsgId and ReturnCode",
      source = SOURCE)
  void regAckPreservesTopicIdMessageIdAndReturnCode() throws Exception {
    RegisterAck ack = assertInstanceOf(
        RegisterAck.class,
        parse(bytes(7, MQTT_SNPacket.REGACK, 0x12, 0x34, 0x45, 0x67, 0x00)));

    assertEquals(0x1234, ack.getTopicId());
    assertEquals(0x4567, ack.getMessageId());
    assertEquals(ReasonCodes.SUCCESS, ack.getStatus());
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section 5.4.13 PUBACK: acknowledgment returns TopicId, MsgId and ReturnCode",
      source = SOURCE)
  void pubAckPreservesTopicIdMessageIdAndReturnCode() throws Exception {
    PubAck ack = assertInstanceOf(
        PubAck.class,
        parse(bytes(7, MQTT_SNPacket.PUBACK, 0x00, 0x2a, 0x12, 0x34, 0x00)));

    assertEquals(42, ack.getTopicId());
    assertEquals(0x1234, ack.getMessageId());
    assertEquals(ReasonCodes.SUCCESS, ack.getStatus());
  }

  @ParameterizedTest
  @MethodSource("qos2AcknowledgementPackets")
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section 5.4.14 PUBREC/PUBREL/PUBCOMP: QoS 2 acknowledgment packets carry the matching MsgId",
      source = SOURCE)
  void qos2AcknowledgementsPreserveMessageId(byte[] wire, Class<? extends MQTT_SNPacket> type)
      throws Exception {
    MQTT_SNPacket packet = parse(wire);
    assertInstanceOf(type, packet);

    int messageId;
    if (packet instanceof PubRec pubRec) {
      messageId = pubRec.getMessageId();
    } else if (packet instanceof PubRel pubRel) {
      messageId = pubRel.getMessageId();
    } else {
      messageId = ((PubComp) packet).getMessageId();
    }
    assertEquals(0x1234, messageId);
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section 5.4.18 UNSUBACK: acknowledgment carries the MsgId of the UNSUBSCRIBE request",
      source = SOURCE)
  void unsubAckPreservesMessageId() throws Exception {
    UnSubAck ack = assertInstanceOf(
        UnSubAck.class,
        parse(bytes(4, MQTT_SNPacket.UNSUBACK, 0x12, 0x34)));

    assertEquals(0x1234, ack.getMsgId());
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section 5.4.20 PINGRESP: response is a two-octet packet containing only Length and MsgType",
      source = SOURCE)
  void pingResponseUsesTwoOctetForm() throws Exception {
    assertInstanceOf(
        PingResponse.class,
        parse(bytes(2, MQTT_SNPacket.PINGRESP)));
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections 5.4.6-5.4.9 Will handshake: WILLTOPIC and WILLMSG carry the prompted Will data",
      source = SOURCE)
  void willTopicAndMessageCarryPromptedWillData() throws Exception {
    WillTopic topic = assertInstanceOf(
        WillTopic.class,
        parse(bytes(8, MQTT_SNPacket.WILLTOPIC, 0x20, 'w', 'i', 'l', 'l', '1')));
    assertEquals("will1", topic.getTopic());
    assertEquals(QualityOfService.AT_LEAST_ONCE, topic.getQoS());

    WillMessage message = assertInstanceOf(
        WillMessage.class,
        parse(bytes(6, MQTT_SNPacket.WILLMSG, 'd', 'a', 't', 'a')));
    assertArrayEquals("data".getBytes(StandardCharsets.UTF_8), message.getMessage());
  }

  private MQTT_SNPacket parse(byte[] wire) throws Exception {
    return factory.parseFrame(new Packet(ByteBuffer.wrap(wire)));
  }

  private static byte[] bytes(int... values) {
    byte[] result = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      result[i] = (byte) values[i];
    }
    return result;
  }

  private static Stream<Arguments> qos2AcknowledgementPackets() {
    return Stream.of(
        Arguments.of(bytes(4, MQTT_SNPacket.PUBREC, 0x12, 0x34), PubRec.class),
        Arguments.of(bytes(4, MQTT_SNPacket.PUBREL, 0x12, 0x34), PubRel.class),
        Arguments.of(bytes(4, MQTT_SNPacket.PUBCOMP, 0x12, 0x34), PubComp.class));
  }

  private static Stream<Arguments> qosFlags() {
    return Stream.of(
        Arguments.of(0, QualityOfService.AT_MOST_ONCE),
        Arguments.of(1, QualityOfService.AT_LEAST_ONCE),
        Arguments.of(2, QualityOfService.EXACTLY_ONCE),
        Arguments.of(3, QualityOfService.MQTT_SN_REGISTERED));
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

  private static Stream<Arguments> reservedTopicTypePackets() {
    return Stream.of(
        Arguments.of((Object) bytes(7, MQTT_SNPacket.PUBLISH, 0x03, 0, 1, 0, 0)),
        Arguments.of((Object) bytes(7, MQTT_SNPacket.SUBSCRIBE, 0x03, 0, 1, 'A', 'B')),
        Arguments.of((Object) bytes(7, MQTT_SNPacket.UNSUBSCRIBE, 0x03, 0, 1, 'A', 'B')));
  }

  private static Stream<Arguments> normalTopicNames() {
    return Stream.of(
        Arguments.of("a"),
        Arguments.of("sensor/temp"),
        Arguments.of("/root/topic"),
        Arguments.of("a/b/c"),
        Arguments.of("+"),
        Arguments.of("#"),
        Arguments.of("ümlaut"),
        Arguments.of("topic with spaces"),
        Arguments.of("$schema/test"),
        Arguments.of("12345678901234567890"));
  }

  private static Stream<Arguments> clientIds() {
    return Stream.of(
        Arguments.of("a"),
        Arguments.of("client-1"),
        Arguments.of("sleeping-node"),
        Arguments.of("node/42"),
        Arguments.of("ümlaut-node"));
  }
}
