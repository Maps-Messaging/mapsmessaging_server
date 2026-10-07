/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MqttSn2ConnectCodecTest {

  @Test
  void decodesReferenceClientConnectLayout() throws Exception {
    ByteBuffer body = ByteBuffer.allocate(9);
    body.put((byte) 0x61).putShort((short) 23).put((byte) 2)
        .putShort((short) 60).putShort((short) 1024).put((byte) 'A').flip();
    MqttSn2ConnectCodec.Connect connect = decode(body);
    assertTrue(connect.cleanStart());
    assertTrue(connect.addressChanges());
    assertTrue(connect.serverSuggestedValues());
    assertFalse(connect.authentication());
    assertEquals(23, connect.packetIdentifier());
    assertEquals(60, connect.keepAliveSeconds());
    assertEquals(1024, connect.maximumPacketSize());
    assertEquals("A", connect.clientIdentifier());
  }

  @Test
  void decodesConnectWithAuthenticationAndCopiesPayload() throws Exception {
    byte[] mechanism = "PLAIN".getBytes(StandardCharsets.UTF_8);
    ByteBuffer body = ByteBuffer.allocate(8 + 1 + mechanism.length + 2 + 3 + 1);
    body.put((byte) 0x05).putShort((short) 44).put((byte) 2)
        .putShort((short) 30).putShort((short) 0);
    body.put((byte) mechanism.length).put(mechanism).putShort((short) 3)
        .put(new byte[]{1, 2, 3}).put((byte) 'B').flip();
    MqttSn2ConnectCodec.Connect connect = decode(body);
    assertEquals("PLAIN", connect.authenticationMethod());
    assertArrayEquals(new byte[]{1, 2, 3}, connect.authenticationData());
    connect.authenticationData()[0] = 99;
    assertArrayEquals(new byte[]{1, 2, 3}, connect.authenticationData());
  }

  @Test
  void rejectsTruncatedOrInvalidConnectBodies() {
    byte[][] inputs = {
        new byte[0],
        {0, 0, 1, 2, 0, 0, 0, 10},
        {0, 0, 1, 1, 0, 1, 0, 10},
        {(byte) 0x80, 0, 1, 2, 0, 1, 0, 10},
        {4, 0, 1, 2, 0, 1, 0, 10},
        {0, 0, 1, 2, 0, 1, 0, 9}
    };
    for (byte[] input : inputs) {
      assertThrows(IOException.class, () -> decode(ByteBuffer.wrap(input)));
    }
  }

  private static MqttSn2ConnectCodec.Connect decode(ByteBuffer body) throws IOException {
    ByteBuffer frame = MqttSn2FrameCodec.encode(MqttSn2PacketType.CONNECT, body);
    return MqttSn2ConnectCodec.decode(MqttSn2FrameCodec.decode(frame));
  }
}
