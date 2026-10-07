/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PacketFactoryV2MalformedPacketTest {

  private final PacketFactoryV2 factory = new PacketFactoryV2();

  @Test
  void malformedConnectIsNotSilentlyConvertedToConnAck() {
    Packet packet = frame(4, MQTT_SNPacket.CONNECT, 0xF8, 2);

    assertThrows(Exception.class, () -> factory.parseFrame(packet));
  }

  @Test
  void malformedPingRequestIsNotSilentlyConvertedToPingResponse() {
    // A client identifier length of ten bytes with no identifier payload.
    Packet packet = frame(5, MQTT_SNPacket.PINGREQ, 0, 0, 10);

    assertThrows(Exception.class, () -> factory.parseFrame(packet));
  }

  @Test
  void emptyPingRequestIsStillParsedAsPingRequest() throws Exception {
    Packet packet = frame(2, MQTT_SNPacket.PINGREQ);

    assertEquals(MQTT_SNPacket.PINGREQ, factory.parseFrame(packet).getControlPacketId());
  }

  private static Packet frame(int... bytes) {
    Packet packet = new Packet(bytes.length, false);
    for (int value : bytes) {
      packet.put((byte) value);
    }
    packet.flip();
    return packet;
  }
}
