/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2PacketDecoder;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.PacketFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MqttSn2ArchitectureTest {

  @Test
  void activeV2ProtocolInheritsOnlyFromCommonProtocol() {
    assertEquals(Protocol.class, MqttSn2Protocol.class.getSuperclass());
    assertFalse(MQTT_SNProtocol.class.isAssignableFrom(MqttSn2Protocol.class));
  }

  @Test
  void independentV2DecoderDoesNotExtendLegacyPacketFactory() {
    assertFalse(PacketFactory.class.isAssignableFrom(MqttSn2PacketDecoder.class));
  }
}
