/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class MqttSn2ConnAckCodecTest {

  @Test
  void minimalSuccessConnAckHasReferenceClientLayout() throws Exception {
    ByteBuffer bytes = MqttSn2ConnAckCodec.encode(
        new MqttSn2ConnAckCodec.ConnAck(false, 7, 0, null, null, null, null, ""));
    assertEquals(6, bytes.remaining());
    assertArrayEquals(new byte[]{6, 2, 0, 0, 7, 0}, array(bytes));
    assertEquals(MqttSn2PacketType.CONNACK, MqttSn2FrameCodec.decode(bytes).type());
  }

  @Test
  void optionalFieldsAreEncodedInSpecificationOrder() {
    ByteBuffer bytes = MqttSn2ConnAckCodec.encode(
        new MqttSn2ConnAckCodec.ConnAck(true, 9, 0, 100L, 30, "PLAIN",
            new byte[]{42}, "assigned"));
    ByteBuffer body = bytes.asReadOnlyBuffer();
    body.position(2);
    assertEquals(15, Byte.toUnsignedInt(body.get()));
    assertEquals(9, Short.toUnsignedInt(body.getShort()));
    assertEquals(0, Byte.toUnsignedInt(body.get()));
    assertEquals(100, Integer.toUnsignedLong(body.getInt()));
    assertEquals(30, Short.toUnsignedInt(body.getShort()));
    assertEquals(5, Byte.toUnsignedInt(body.get()));
  }

  @Test
  void invalidResponsesAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> MqttSn2ConnAckCodec.encode(
        new MqttSn2ConnAckCodec.ConnAck(true, 10, 128, null, null, null, null, "")));
    assertThrows(IllegalArgumentException.class, () -> MqttSn2ConnAckCodec.encode(
        new MqttSn2ConnAckCodec.ConnAck(false, 0, 0, null, null, null, null, "")));
    assertThrows(IllegalArgumentException.class, () -> MqttSn2ConnAckCodec.encode(
        new MqttSn2ConnAckCodec.ConnAck(false, 1, 0, null, 0, null, null, "")));
  }

  private static byte[] array(ByteBuffer buffer) {
    ByteBuffer source = buffer.asReadOnlyBuffer();
    byte[] output = new byte[source.remaining()];
    source.get(output);
    return output;
  }
}
