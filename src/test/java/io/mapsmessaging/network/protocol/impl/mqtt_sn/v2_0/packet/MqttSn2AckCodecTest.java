/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class MqttSn2AckCodecTest {

  @Test
  void allSimpleAckPacketsRoundTrip() throws Exception {
    for (MqttSn2PacketType type : new MqttSn2PacketType[]{
        MqttSn2PacketType.PUBACK, MqttSn2PacketType.PUBREC,
        MqttSn2PacketType.PUBREL, MqttSn2PacketType.PUBCOMP,
        MqttSn2PacketType.UNSUBACK}) {
      for (Integer reason : new Integer[]{null, 0, 0x80}) {
        MqttSn2AckCodec.Ack ack = new MqttSn2AckCodec.Ack(type, 100, reason);
        ByteBuffer encoded = MqttSn2AckCodec.encode(ack);
        assertEquals(type.code(), Byte.toUnsignedInt(encoded.get(1)));
        assertEquals(ack, MqttSn2AckCodec.decode(MqttSn2FrameCodec.decode(encoded)));
      }
    }
  }

  @Test
  void rejectsInvalidIdentifiersAndLengths() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> MqttSn2AckCodec.encode(
        new MqttSn2AckCodec.Ack(MqttSn2PacketType.PUBACK, 0, null)));
    assertThrows(IllegalArgumentException.class, () -> MqttSn2AckCodec.encode(
        new MqttSn2AckCodec.Ack(MqttSn2PacketType.CONNECT, 1, null)));
    for (byte[] bytes : new byte[][]{
        {4, 4, 0, 0},
        {3, 4, 1},
        {6, 4, 0, 1, 0, 0}
    }) {
      assertThrows(IOException.class, () ->
          MqttSn2AckCodec.decode(MqttSn2FrameCodec.decode(ByteBuffer.wrap(bytes))));
    }
  }
}
