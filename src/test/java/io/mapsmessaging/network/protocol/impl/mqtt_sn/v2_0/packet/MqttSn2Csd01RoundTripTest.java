/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;

class MqttSn2Csd01RoundTripTest {

  @Test
  void connectRoundTripsAuthenticationFields() throws Exception {
    MqttSn2ConnectCodec.Connect source = new MqttSn2ConnectCodec.Connect(
        true, false, true, true, false, 31, 60, 4096,
        "sensor-42", "PLAIN", new byte[]{1, 2, 3});
    ByteBuffer wire = MqttSn2ConnectCodec.encode(source);
    MqttSn2ConnectCodec.Connect decoded =
        MqttSn2ConnectCodec.decode(MqttSn2FrameCodec.decode(wire));
    assertEquals(source.packetIdentifier(), decoded.packetIdentifier());
    assertEquals(source.clientIdentifier(), decoded.clientIdentifier());
    assertEquals(source.authenticationMethod(), decoded.authenticationMethod());
    assertArrayEquals(source.authenticationData(), decoded.authenticationData());
  }

  @Test
  void pingAndSleepRequestsRoundTrip() throws Exception {
    ByteBuffer ping = MqttSn2ControlCodec.encodePingRequest(28);
    assertEquals(28, MqttSn2ControlCodec.decodePingRequest(MqttSn2FrameCodec.decode(ping)));
    MqttSn2SleepCodec.SleepRequest request =
        new MqttSn2SleepCodec.SleepRequest(true, 28, 1000);
    assertEquals(request, MqttSn2SleepCodec.decodeRequest(
        MqttSn2FrameCodec.decode(MqttSn2SleepCodec.encodeRequest(request))));
  }

  @Test
  void protectionEnvelopeStructuralRoundTripRequiresTagProvider() throws Exception {
    MqttSn2ProtectionCodec.Envelope source = new MqttSn2ProtectionCodec.Envelope(
        1, 1, new byte[]{0, 0, 0, 0, 0, 0, 0, 1},
        new byte[]{1, 2, 3, 4}, new byte[0], new byte[0], new byte[0],
        new byte[]{4, 12, 0, 1}, new byte[]{11, 12, 13, 14});
    ByteBuffer wire = MqttSn2ProtectionCodec.encode(source);
    MqttSn2ProtectionCodec.Envelope decoded =
        MqttSn2ProtectionCodec.decode(wire, (scheme, tagCode) -> 4);
    assertEquals(1, decoded.scheme());
    assertArrayEquals(source.senderIdentifier(), decoded.senderIdentifier());
    assertArrayEquals(source.random(), decoded.random());
    assertArrayEquals(source.protectedPacket(), decoded.protectedPacket());
    assertArrayEquals(source.authenticationTag(), decoded.authenticationTag());
    decoded.protectedPacket()[0] = 99;
    assertArrayEquals(source.protectedPacket(), decoded.protectedPacket());
  }
}
