package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.io.Packet;
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

class MqttSn12SpecificationComplianceTest {

  private final PacketFactory factory = new PacketFactory();

  @Test
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
  void publishDecodesAllMqttSn12QosBitPatterns(
      int qosBits, QualityOfService expected) throws Exception {
    Publish publish = assertInstanceOf(
        Publish.class,
        parse(bytes(7, MQTT_SNPacket.PUBLISH, qosBits << 5, 0, 7, 0, 0)));

    assertEquals(expected, publish.getQoS());
  }

  @Test
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
  void reservedTopicIdTypeIsRejected(byte[] wire) {
    assertThrows(
        IOException.class,
        () -> parse(wire),
        "TopicIdType=0b11 is reserved in MQTT-SN 1.2 and must not be accepted as a valid topic selector");
  }

  @ParameterizedTest
  @MethodSource("normalTopicNames")
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
  void pingRequestMayCarryClientIdForSleepingClient(String clientId) throws Exception {
    byte[] client = clientId.getBytes(StandardCharsets.UTF_8);
    byte[] wire = new byte[2 + client.length];
    wire[0] = (byte) wire.length;
    wire[1] = (byte) MQTT_SNPacket.PINGREQ;
    System.arraycopy(client, 0, wire, 2, client.length);

    PingRequest ping = assertInstanceOf(PingRequest.class, parse(wire));

    assertEquals(clientId, ping.getClientId());
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
