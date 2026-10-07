package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.conformance.common.ProtocolRequirement;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

@Tag("conformance")
@Tag("conformance-core")
@Tag("mqtt-sn")
class MqttSn12WireValidationSpecificationTest {

  private static final String SPEC = "MQTT-SN Version 1.2";
  private static final String SOURCE = ProtocolRequirement.MQTT_SN_12_SOURCE;

  private final PacketFactory factory = new PacketFactory();

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section 5.2.1 Length: Length is the total number of octets in the MQTT-SN message",
      source = SOURCE)
  void datagramShorterThanDeclaredLengthIsRejected() {
    assertThrows(
        IOException.class,
        () -> parse(bytes(7, MQTT_SNPacket.PUBLISH, 0, 0, 1, 0)));
  }

  @Test
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section 5.2.1 Length: datagram bytes beyond the declared MQTT-SN message length are not part of that message",
      source = SOURCE)
  void datagramLongerThanDeclaredLengthIsRejected() {
    assertThrows(
        IOException.class,
        () -> parse(bytes(2, MQTT_SNPacket.PINGRESP, 0)));
  }

  @Test
  @Disabled("Known MQTT-SN 1.2 empty WILLTOPIC parsing gap: MSG-404")
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections 5.4.7 and 6.4 Will handling: an empty WILLTOPIC is exactly two octets and deletes the Will topic and message",
      source = SOURCE)
  void emptyWillTopicTwoOctetFormIsAccepted() throws Exception {
    WillTopic topic = assertInstanceOf(
        WillTopic.class,
        parse(bytes(2, MQTT_SNPacket.WILLTOPIC)));

    assertTrue(topic.getTopic() == null || topic.getTopic().isEmpty());
  }

  @Test
  @Disabled("Known MQTT-SN 1.2 empty WILLTOPICUPD parsing gap: MSG-404")
  @ProtocolRequirement(
      specification = SPEC,
      value = "Sections 5.4.22 and 6.4 Will update: an empty WILLTOPICUPD is exactly two octets and deletes the Will topic and message",
      source = SOURCE)
  void emptyWillTopicUpdateTwoOctetFormIsAccepted() throws Exception {
    WillTopicUpdate update = assertInstanceOf(
        WillTopicUpdate.class,
        parse(bytes(2, MQTT_SNPacket.WILLTOPICUPD)));

    assertTrue(update.getTopic() == null || update.getTopic().isEmpty());
  }

  @Test
  @Disabled("Known MQTT-SN 1.2 extended-length parsing gap: MSG-405")
  @ProtocolRequirement(
      specification = SPEC,
      value = "Section 5.2.1 Length: MQTT-SN 1.2 extended length supports messages greater than 255 octets",
      source = SOURCE)
  void extendedLengthRegisterPreservesPayloadBoundaries() throws Exception {
    String topicName = "x".repeat(260);
    byte[] topic = topicName.getBytes();
    int wireLength = 8 + topic.length;
    byte[] wire = new byte[wireLength];
    wire[0] = 1;
    wire[1] = (byte) ((wireLength >>> 8) & 0xff);
    wire[2] = (byte) (wireLength & 0xff);
    wire[3] = (byte) MQTT_SNPacket.REGISTER;
    wire[4] = 0;
    wire[5] = 0;
    wire[6] = 0x12;
    wire[7] = 0x34;
    System.arraycopy(topic, 0, wire, 8, topic.length);

    Register register = assertInstanceOf(Register.class, parse(wire));

    assertEquals(0, register.getTopicId());
    assertEquals(0x1234, register.getMessageId());
    assertEquals(topicName, register.getTopic());
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
}
