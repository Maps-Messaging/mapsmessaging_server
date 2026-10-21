package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class UnsubscribeTest {

  @Test
  void parsesTopicNameSubscription() {
    Packet packet = new Packet(32, false);
    packet.put((byte) MQTT_SNPacket.TOPIC_NAME);
    packet.putShort(0x1234);
    packet.put("sensor/temp".getBytes(StandardCharsets.UTF_8));
    packet.flip();

    Unsubscribe unsubscribe = new Unsubscribe(packet);

    assertEquals(0x1234, unsubscribe.getMsgId());
    assertEquals(MQTT_SNPacket.TOPIC_NAME, unsubscribe.topicIdType());
    assertEquals("sensor/temp", unsubscribe.getTopicName());
  }

  @Test
  void parsesPredefinedTopicId() {
    Packet packet = new Packet(8, false);
    packet.put((byte) MQTT_SNPacket.TOPIC_PRE_DEFINED_ID);
    packet.putShort(7);
    packet.putShort(42);
    packet.flip();

    Unsubscribe unsubscribe = new Unsubscribe(packet);

    assertEquals(7, unsubscribe.getMsgId());
    assertEquals(42, unsubscribe.getTopicId());
    assertNull(unsubscribe.getTopicName());
  }

  @Test
  void settingTopicIdTypeReplacesExistingTypeBits() {
    Packet packet = new Packet(8, false);
    packet.put((byte) MQTT_SNPacket.TOPIC_PRE_DEFINED_ID);
    packet.putShort(1);
    packet.putShort(2);
    packet.flip();

    Unsubscribe unsubscribe = new Unsubscribe(packet);
    unsubscribe.setTopicIdType(MQTT_SNPacket.TOPIC_SHORT_NAME);

    assertEquals(
        MQTT_SNPacket.TOPIC_SHORT_NAME,
        unsubscribe.topicIdType(),
        "setTopicIdType should replace the two topic-type bits, not OR with the old value"
    );
  }
}
