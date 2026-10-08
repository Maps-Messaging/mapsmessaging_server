/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2AckCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2FrameCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2PacketType;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2PublishCodec;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** CSD01 MQTT-SN 4.3.3-1, 4.3.4-1..8, and 4.4.2-1..7 behavior. */
class MqttSn2OutgoingDeliveryManagerTest {

  @Test
  void qos_two_delivery_uses_pubrec_pubrel_pubcomp_before_releasing_next_publish() throws Exception {
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 3);
    AtomicInteger firstCompleted = new AtomicInteger();
    AtomicInteger secondCompleted = new AtomicInteger();

    ByteBuffer first = manager.enqueue("sensor/one", new byte[]{1}, 2, false,
        firstCompleted::incrementAndGet, 1_000);
    ByteBuffer second = manager.enqueue("sensor/two", new byte[]{2}, 1, false,
        secondCompleted::incrementAndGet, 1_000);

    assertNotNull(first);
    assertNull(second, "CSD01 permits one outstanding request packet per sender");
    MqttSn2PublishCodec.Publish sent = MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(first));
    assertEquals(2, sent.qos());
    int packetIdentifier = sent.packetIdentifier();
    assertTrue(packetIdentifier > 0);

    ByteBuffer pubRel = manager.acknowledge(new MqttSn2AckCodec.Ack(
        MqttSn2PacketType.PUBREC, packetIdentifier, null), 1_010);
    assertNotNull(pubRel);
    assertEquals(MqttSn2PacketType.PUBREL, MqttSn2FrameCodec.decode(pubRel).type());
    assertEquals(0, firstCompleted.get());

    ByteBuffer next = manager.acknowledge(new MqttSn2AckCodec.Ack(
        MqttSn2PacketType.PUBCOMP, packetIdentifier, null), 1_020);
    assertEquals(1, firstCompleted.get());
    assertNotNull(next);
    assertEquals(1, MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(next)).qos());
    assertEquals(0, secondCompleted.get());
  }

  @Test
  void qos_two_retries_pubrel_without_retransmitting_publish_after_pubrec() throws Exception {
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 3);
    ByteBuffer publish = manager.enqueue("sensor/one", new byte[]{1}, 2, false, null, 1_000);
    int packetIdentifier = MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(publish)).packetIdentifier();
    ByteBuffer pubRel = manager.acknowledge(new MqttSn2AckCodec.Ack(
        MqttSn2PacketType.PUBREC, packetIdentifier, null), 1_001);

    ByteBuffer retry = manager.retryExpired(1_101);

    assertEquals(MqttSn2FrameCodec.decode(pubRel).type(), MqttSn2FrameCodec.decode(retry).type());
    assertEquals(packetIdentifier, MqttSn2AckCodec.decode(MqttSn2FrameCodec.decode(retry)).packetIdentifier());
  }

  @Test
  void failed_pubrec_releases_identifier_without_success_callback() throws Exception {
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 3);
    AtomicInteger completed = new AtomicInteger();
    ByteBuffer publish = manager.enqueue("sensor/one", new byte[]{1}, 2, false,
        completed::incrementAndGet, 1_000);
    int packetIdentifier = MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(publish)).packetIdentifier();

    ByteBuffer next = manager.acknowledge(new MqttSn2AckCodec.Ack(
        MqttSn2PacketType.PUBREC, packetIdentifier, 0x80), 1_001);

    assertNull(next);
    assertEquals(0, completed.get());
    assertFalse(manager.hasInFlightDelivery());
  }

  @Test
  void negative_puback_never_completes_delivery_as_success() throws Exception {
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager();
    AtomicInteger completed = new AtomicInteger();
    ByteBuffer first = manager.enqueue("sensor/one", new byte[]{1}, 1, false,
        completed::incrementAndGet, 1_000);
    int id = MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(first)).packetIdentifier();
    manager.acknowledge(new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBACK, id, 0x80), 1_001);
    assertEquals(0, completed.get());
    assertFalse(manager.hasInFlightDelivery());
  }

  @Test
  void qos_one_retry_sets_dup_and_reuses_packet_identifier() throws Exception {
    MqttSn2OutgoingDeliveryManager manager = new MqttSn2OutgoingDeliveryManager(100, 2);
    ByteBuffer initial = manager.enqueue("sensor/one", new byte[]{1}, 1, false, () -> {}, 1_000);
    MqttSn2PublishCodec.Publish sent = MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(initial));

    ByteBuffer retry = manager.retryExpired(1_100);
    MqttSn2PublishCodec.Publish resent = MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(retry));
    assertTrue(resent.duplicate());
    assertEquals(sent.packetIdentifier(), resent.packetIdentifier());
    assertArrayEquals(sent.payload(), resent.payload());
  }
}
