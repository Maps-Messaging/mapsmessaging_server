/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class MqttSn2PacketDecoderTest {

  @Test
  void decodingUsesV2IdentifiersAndTypedPacketBodies() throws Exception {
    MqttSn2PacketDecoder.Decoded ping = MqttSn2PacketDecoder.decode(
        ByteBuffer.wrap(new byte[]{4, 12, 0, 42}));
    assertEquals(MqttSn2PacketType.PINGREQ, ping.type());
    assertEquals(42, ping.content());

    MqttSn2PacketDecoder.Decoded publish = MqttSn2PacketDecoder.decode(
        ByteBuffer.wrap(new byte[]{6, 18, 1, 0, 7, 42}));
    assertEquals(MqttSn2PacketType.PUBWOS, publish.type());
    assertInstanceOf(MqttSn2PublishCodec.Publish.class, publish.content());
    assertEquals(7, ((MqttSn2PublishCodec.Publish) publish.content()).topicAlias());
  }

  @Test
  void neverFallsBackToLegacyPacketNumberingOrUnknownDecoders() {
    assertThrows(IOException.class, () -> MqttSn2PacketDecoder.decode(
        ByteBuffer.wrap(new byte[]{2, 25})));
    assertThrows(IOException.class, () -> MqttSn2PacketDecoder.decode(
        ByteBuffer.wrap(new byte[]{2, 17})));
    assertThrows(IOException.class, () -> MqttSn2PacketDecoder.decode(
        ByteBuffer.wrap(new byte[]{2, (byte) 255})));
  }

  @Test
  void rejectsMalformedPacketsBeforeSessionHandling() {
    assertThrows(IOException.class, () -> MqttSn2PacketDecoder.decode(
        ByteBuffer.wrap(new byte[]{4, 12, 0, 0})));
    assertThrows(IOException.class, () -> MqttSn2PacketDecoder.decode(
        ByteBuffer.wrap(new byte[]{4, 1, 0, 0})));
  }
}
