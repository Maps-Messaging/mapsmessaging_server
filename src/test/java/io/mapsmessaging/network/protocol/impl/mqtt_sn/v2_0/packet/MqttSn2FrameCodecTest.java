/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class MqttSn2FrameCodecTest {

  @Test
  void shortHeaderMatchesReferenceWireFormat() throws Exception {
    ByteBuffer frame = MqttSn2FrameCodec.encode(MqttSn2PacketType.PINGREQ, ByteBuffer.allocate(0));
    assertArrayEquals(new byte[]{2, 12}, toArray(frame));
    assertEquals(MqttSn2PacketType.PINGREQ, MqttSn2FrameCodec.decode(frame).type());
  }

  @Test
  void extendedHeaderMatchesReferenceWireFormat() throws Exception {
    ByteBuffer frame = MqttSn2FrameCodec.encode(MqttSn2PacketType.PUBLISH, ByteBuffer.allocate(254));
    assertEquals(258, frame.remaining());
    assertEquals(1, Byte.toUnsignedInt(frame.get(0)));
    assertEquals(258, Short.toUnsignedInt(frame.getShort(1)));
    assertEquals(3, Byte.toUnsignedInt(frame.get(3)));
    assertEquals(254, MqttSn2FrameCodec.decode(frame).payload().remaining());
  }

  @Test
  void decodePreservesSourcePosition() throws Exception {
    ByteBuffer source = ByteBuffer.wrap(new byte[]{99, 3, 15, 7});
    source.position(1);
    assertEquals(MqttSn2PacketType.AUTH, MqttSn2FrameCodec.decode(source).type());
    assertEquals(1, source.position());
  }

  @Test
  void rejectsMalformedLengthsAndReservedTypes() {
    for (byte[] bytes : new byte[][]{
        {},
        {1},
        {1, 0, 3, 1},
        {1, 0, 4},
        {0, 1},
        {2, 0},
        {3, 1},
        {4, 1},
        {2, (byte) 253}
    }) {
      assertThrows(IOException.class, () -> MqttSn2FrameCodec.decode(ByteBuffer.wrap(bytes)));
    }
  }

  @Test
  void rejectsOversizedFrames() {
    assertThrows(IllegalArgumentException.class, () ->
        MqttSn2FrameCodec.encode(MqttSn2PacketType.PUBLISH, ByteBuffer.allocate(65532)));
  }

  private static byte[] toArray(ByteBuffer bytes) {
    ByteBuffer view = bytes.asReadOnlyBuffer();
    byte[] result = new byte[view.remaining()];
    view.get(result);
    return result;
  }
}
