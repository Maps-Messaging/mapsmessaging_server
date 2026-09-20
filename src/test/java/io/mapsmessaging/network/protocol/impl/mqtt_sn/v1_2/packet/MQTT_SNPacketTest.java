package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MQTT_SNPacketTest {

  @Test
  void completeRunsCallbackAtMostOnce() {
    MQTT_SNPacket packet = new MQTT_SNPacket(MQTT_SNPacket.PUBLISH);
    AtomicInteger calls = new AtomicInteger();
    packet.setCallback(calls::incrementAndGet);

    packet.complete();
    packet.complete();

    assertEquals(1, calls.get());
    assertNull(packet.getCallback());
  }

  @Test
  void completeWithoutCallbackIsSafe() {
    MQTT_SNPacket packet = new MQTT_SNPacket(MQTT_SNPacket.PINGREQ);

    assertDoesNotThrow(packet::complete);
  }

  @Test
  void shortLengthUsesSingleByteEncoding() {
    MQTT_SNPacket mqtt = new MQTT_SNPacket(0);
    Packet packet = new Packet(8, false);

    assertEquals(42, mqtt.packLength(packet, 42));
    assertEquals(1, packet.position());

    packet.flip();
    assertEquals(42, packet.getByte());
  }

  @Test
  void extendedLengthUsesMarkerAndBigEndianLengthIncludingHeader() {
    MQTT_SNPacket mqtt = new MQTT_SNPacket(0);
    Packet packet = new Packet(8, false);

    assertEquals(300, mqtt.packLength(packet, 300));
    assertEquals(3, packet.position());

    packet.flip();
    assertEquals(1, packet.getByte());
    assertEquals(302, packet.getShort());
  }
}
