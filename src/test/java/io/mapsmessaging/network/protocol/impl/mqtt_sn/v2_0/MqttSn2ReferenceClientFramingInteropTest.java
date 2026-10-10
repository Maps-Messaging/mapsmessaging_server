/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import static org.junit.jupiter.api.Assertions.*;

import io.mapsmessaging.mqttsn.ConnectOptions;
import io.mapsmessaging.mqttsn.MqttSnCodec;
import io.mapsmessaging.mqttsn.PacketType;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2FrameCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2PacketType;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Client and server implement packet length/type framing independently. */
class MqttSn2ReferenceClientFramingInteropTest {
  @Test void clientConnectFramingIsAcceptedByServer() throws Exception {
    byte[] wire = MqttSnCodec.encodeConnect(new ConnectOptions(
        true, false, false, 1, 60, 0, "interop-device"));
    var decoded = MqttSn2FrameCodec.decode(ByteBuffer.wrap(wire));
    assertEquals(MqttSn2PacketType.CONNECT, decoded.type());
    assertEquals(wire.length, decoded.packetLength());
  }

  @Test void clientAndServerAgreeOnShortAndExtendedFrames() throws Exception {
    for (int payloadLength : new int[] {0, 1, 32, 252, 253, 254, 255, 512, 4096}) {
      byte[] payload = new byte[payloadLength];
      Arrays.fill(payload, (byte) 0x42);
      byte[] fromClient = MqttSnCodec.encode(PacketType.PINGRESP, payload);
      var server = MqttSn2FrameCodec.decode(ByteBuffer.wrap(fromClient));
      assertEquals(payloadLength, server.payload().remaining());
      assertEquals(MqttSn2PacketType.PINGRESP, server.type());
      ByteBuffer fromServer = MqttSn2FrameCodec.encode(
          MqttSn2PacketType.PINGRESP, ByteBuffer.wrap(payload));
      byte[] serverWire = new byte[fromServer.remaining()];
      fromServer.get(serverWire);
      assertEquals(serverWire.length, MqttSnCodec.decode(ByteBuffer.wrap(serverWire)).packetLength());
      assertArrayEquals(fromClient, serverWire);
    }
  }
}
