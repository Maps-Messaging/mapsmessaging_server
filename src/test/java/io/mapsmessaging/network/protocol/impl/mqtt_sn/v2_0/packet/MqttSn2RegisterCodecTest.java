/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class MqttSn2RegisterCodecTest {

  @Test
  void registerUsesCsd01WireLayout() throws Exception {
    ByteBuffer encoded = MqttSn2RegisterCodec.encode(
        new MqttSn2RegisterCodec.Register(42, "sensors/room"));
    assertEquals(16, Byte.toUnsignedInt(encoded.get(1)));
    MqttSn2RegisterCodec.Register decoded =
        MqttSn2RegisterCodec.decode(MqttSn2FrameCodec.decode(encoded));
    assertEquals(42, decoded.packetIdentifier());
    assertEquals("sensors/room", decoded.topicName());
  }

  @Test
  void rejectsInvalidIdentifiersFlagsAndTopicNames() {
    assertThrows(IllegalArgumentException.class, () ->
        MqttSn2RegisterCodec.encode(new MqttSn2RegisterCodec.Register(0, "ok")));
    assertThrows(IllegalArgumentException.class, () ->
        MqttSn2RegisterCodec.encode(new MqttSn2RegisterCodec.Register(1, "bad/+")));
    for (byte[] bytes : new byte[][]{
        {5, 16, 1, 0, 1},
        {5, 16, 0, 0, 0},
        {5, 16, 0, 0, 1},
        {6, 16, 0, 0, 1, (byte) 0xFF}
    }) {
      assertThrows(IOException.class, () ->
          MqttSn2RegisterCodec.decode(MqttSn2FrameCodec.decode(ByteBuffer.wrap(bytes))));
    }
  }
}
