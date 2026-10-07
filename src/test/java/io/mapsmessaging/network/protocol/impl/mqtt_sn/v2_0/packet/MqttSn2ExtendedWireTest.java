/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class MqttSn2ExtendedWireTest {

  @Test
  void publishAndPubwosEncodeRoundTrip() throws Exception {
    MqttSn2PublishCodec.Publish qosOne =
        new MqttSn2PublishCodec.Publish(false, 1, false, true, 0, 27, "sensor/temp",
            0, new byte[]{10, 20, 30});
    ByteBuffer encoded = MqttSn2PublishCodec.encode(qosOne);
    assertEquals(3, Byte.toUnsignedInt(encoded.get(1)));
    MqttSn2PublishCodec.Publish decoded =
        MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(encoded));
    assertEquals(qosOne.qos(), decoded.qos());
    assertEquals(qosOne.packetIdentifier(), decoded.packetIdentifier());
    assertEquals(qosOne.topicName(), decoded.topicName());
    assertArrayEquals(qosOne.payload(), decoded.payload());

    MqttSn2PublishCodec.Publish wos =
        new MqttSn2PublishCodec.Publish(true, 0, false, false, 2, 0, null, 15, new byte[]{7});
    MqttSn2PublishCodec.Publish decodedWos =
        MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(MqttSn2PublishCodec.encode(wos)));
    assertTrue(decodedWos.withoutSession());
    assertEquals(15, decodedWos.topicAlias());
  }

  @Test
  void regAckAndConnAckAreDispatchedIndependently() throws Exception {
    ByteBuffer regAck = MqttSn2RegAckCodec.encode(new MqttSn2RegAckCodec.RegAck(2, 12, 4, 0));
    MqttSn2PacketDecoder.Decoded decoded = MqttSn2PacketDecoder.decode(regAck);
    assertEquals(MqttSn2PacketType.REGACK, decoded.type());
    assertInstanceOf(MqttSn2RegAckCodec.RegAck.class, decoded.content());

    ByteBuffer connAck = MqttSn2ConnAckCodec.encode(new MqttSn2ConnAckCodec.ConnAck(
        false, 7, 0, null, null, null, null, ""));
    assertEquals(MqttSn2PacketType.CONNACK, MqttSn2PacketDecoder.decode(connAck).type());
  }

  @Test
  void protectionEnvelopeRequiresProviderToInterpretTag() throws Exception {
    byte[] bytes = {
        20, (byte) 255, 0x10, 1,
        0, 0, 0, 0, 0, 0, 0, 1,
        0, 0, 0, 2,
        2, 21,
        11, 12
    };
    MqttSn2ProtectionCodec.Envelope envelope =
        MqttSn2ProtectionCodec.decode(ByteBuffer.wrap(bytes), (scheme, code) -> 2);
    assertEquals(1, envelope.scheme());
    assertArrayEquals(new byte[]{2, 21}, envelope.protectedPacket());
    assertArrayEquals(new byte[]{11, 12}, envelope.authenticationTag());

    assertThrows(IOException.class, () ->
        MqttSn2ProtectionCodec.decode(ByteBuffer.wrap(bytes), (scheme, code) -> 99));
    assertThrows(IOException.class, () ->
        MqttSn2PacketDecoder.decode(ByteBuffer.wrap(bytes)));
  }
}
