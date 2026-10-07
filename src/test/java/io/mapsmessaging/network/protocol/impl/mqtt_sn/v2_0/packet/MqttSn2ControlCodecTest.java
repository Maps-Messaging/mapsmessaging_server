/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class MqttSn2ControlCodecTest {

  @Test
  void pingReqUsesV2PacketIdentifier() throws Exception {
    MqttSn2FrameCodec.Frame frame = MqttSn2FrameCodec.decode(
        ByteBuffer.wrap(new byte[]{4, 12, 0x12, 0x34}));
    assertEquals(0x1234, MqttSn2ControlCodec.decodePingRequest(frame));
    assertThrows(IOException.class, () -> MqttSn2ControlCodec.decodePingRequest(
        MqttSn2FrameCodec.decode(ByteBuffer.wrap(new byte[]{4, 12, 0, 0}))));
  }

  @Test
  void pingRespRoundTripsWithOptionalRemainingMessages() throws Exception {
    ByteBuffer bytes = MqttSn2ControlCodec.encodePingResponse(42, 7);
    assertArrayEquals(new byte[]{5, 13, 0, 42, 7}, copy(bytes));
    assertEquals(new MqttSn2ControlCodec.PingResponse(42, 7),
        MqttSn2ControlCodec.decodePingResponse(MqttSn2FrameCodec.decode(bytes)));

    ByteBuffer minimal = MqttSn2ControlCodec.encodePingResponse(42, null);
    assertEquals(new MqttSn2ControlCodec.PingResponse(42, null),
        MqttSn2ControlCodec.decodePingResponse(MqttSn2FrameCodec.decode(minimal)));
    assertThrows(IllegalArgumentException.class, () -> MqttSn2ControlCodec.encodePingResponse(0, null));
  }

  @Test
  void authRoundTripsAndDefensivelyCopiesData() throws Exception {
    byte[] source = {1, 2, 3};
    MqttSn2ControlCodec.Auth auth = new MqttSn2ControlCodec.Auth(17, 0x18, "PLAIN", source);
    source[0] = 99;
    auth.data()[1] = 99;
    ByteBuffer bytes = MqttSn2ControlCodec.encodeAuth(auth);
    assertEquals(15, Byte.toUnsignedInt(bytes.get(1)));
    MqttSn2ControlCodec.Auth decoded = MqttSn2ControlCodec.decodeAuth(MqttSn2FrameCodec.decode(bytes));
    assertEquals(17, decoded.packetIdentifier());
    assertEquals(0x18, decoded.reasonCode());
    assertEquals("PLAIN", decoded.mechanism());
    assertArrayEquals(new byte[]{1, 2, 3}, decoded.data());
  }

  @Test
  void authRejectsTruncationAndInvalidUtf8() throws Exception {
    assertThrows(IOException.class, () -> MqttSn2ControlCodec.decodeAuth(
        MqttSn2FrameCodec.decode(ByteBuffer.wrap(new byte[]{6, 15, 0, 1, 0, 1}))));
    assertThrows(IOException.class, () -> MqttSn2ControlCodec.decodeAuth(
        MqttSn2FrameCodec.decode(ByteBuffer.wrap(new byte[]{7, 15, 0, 1, 0, 1, (byte) 0xFF}))));
    assertThrows(IllegalArgumentException.class, () ->
        new MqttSn2ControlCodec.Auth(0, 0, "PLAIN", new byte[0]));
  }

  private static byte[] copy(ByteBuffer buffer) {
    ByteBuffer source = buffer.asReadOnlyBuffer();
    byte[] data = new byte[source.remaining()];
    source.get(data);
    return data;
  }
}
