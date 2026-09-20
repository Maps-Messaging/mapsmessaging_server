package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class UnsubscribeTest {

  @Test
  void parsesNormalAndLongTopicNames() {
    for (int type : new int[]{MQTT_SNPacket.TOPIC_NAME, MQTT_SNPacket.LONG_TOPIC_NAME}) {
      Packet packet = new Packet(32, false);
      packet.put((byte) type);
      packet.putShort(11);
      packet.put("vehicle/state".getBytes(StandardCharsets.UTF_8));
      packet.flip();

      Unsubscribe unsubscribe = new Unsubscribe(packet);

      assertEquals(11, unsubscribe.getMsgId());
      assertEquals("vehicle/state", unsubscribe.getTopicName());
      assertEquals(-1, unsubscribe.getTopicId());
    }
  }

  @Test
  void parsesNumericTopicIdentifiers() {
    for (int type : new int[]{
        MQTT_SNPacket.TOPIC_PRE_DEFINED_ID,
        MQTT_SNPacket.TOPIC_SHORT_NAME}) {
      Packet packet = new Packet(8, false);
      packet.put((byte) type);
      packet.putShort(12);
      packet.putShort(99);
      packet.flip();

      Unsubscribe unsubscribe = new Unsubscribe(packet);

      assertEquals(12, unsubscribe.getMsgId());
      assertEquals(99, unsubscribe.getTopicId());
      assertNull(unsubscribe.getTopicName());
    }
  }
}
