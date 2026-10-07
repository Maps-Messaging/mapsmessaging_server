/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class MqttSn2GatewayCodecTest {

  @Test
  void gatewayDiscoveryMatchesReferenceClientPacketLayout() throws Exception {
    ByteBuffer advertise = MqttSn2GatewayCodec.encodeAdvertise(
        new MqttSn2GatewayCodec.Advertise(12, 300));
    assertArrayEquals(new byte[]{5, 22, 12, 1, 44}, array(advertise));
    assertEquals(new MqttSn2GatewayCodec.Advertise(12, 300),
        MqttSn2GatewayCodec.decodeAdvertise(MqttSn2FrameCodec.decode(advertise)));

    ByteBuffer search = MqttSn2GatewayCodec.encodeSearchGateway(5);
    assertArrayEquals(new byte[]{3, 23, 5}, array(search));
    assertEquals(5, MqttSn2GatewayCodec.decodeSearchGateway(MqttSn2FrameCodec.decode(search)));

    byte[] address = {1, 2, 3};
    MqttSn2GatewayCodec.GatewayInfo info = new MqttSn2GatewayCodec.GatewayInfo(17, address);
    address[0] = 99;
    ByteBuffer response = MqttSn2GatewayCodec.encodeGatewayInfo(info);
    assertArrayEquals(new byte[]{6, 24, 17, 1, 2, 3}, array(response));
    assertArrayEquals(new byte[]{1, 2, 3},
        MqttSn2GatewayCodec.decodeGatewayInfo(MqttSn2FrameCodec.decode(response)).address());
  }

  @Test
  void rejectsInvalidLengthsAndRange() {
    assertThrows(IllegalArgumentException.class, () ->
        MqttSn2GatewayCodec.encodeSearchGateway(256));
    assertThrows(IllegalArgumentException.class, () ->
        MqttSn2GatewayCodec.encodeAdvertise(new MqttSn2GatewayCodec.Advertise(1, 65536)));
    for (byte[] invalid : new byte[][]{
        {2, 22}, {4, 23, 1, 2}, {2, 24}
    }) {
      assertThrows(IOException.class, () ->
          MqttSn2GatewayCodec.decodeAdvertise(MqttSn2FrameCodec.decode(ByteBuffer.wrap(invalid))));
    }
    assertThrows(IOException.class, () ->
        MqttSn2GatewayCodec.decodeGatewayInfo(
            MqttSn2FrameCodec.decode(ByteBuffer.wrap(new byte[]{2, 24}))));
  }

  private static byte[] array(ByteBuffer buffer) {
    ByteBuffer source = buffer.asReadOnlyBuffer();
    byte[] result = new byte[source.remaining()];
    source.get(result);
    return result;
  }
}
