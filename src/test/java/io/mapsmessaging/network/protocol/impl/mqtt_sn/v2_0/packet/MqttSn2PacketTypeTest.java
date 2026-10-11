/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MqttSn2PacketTypeTest {

  @Test
  void everyDefinedTypeRoundTripsThroughWireIdentifier() {
    for (MqttSn2PacketType type : MqttSn2PacketType.values()) {
      assertEquals(type, MqttSn2PacketType.fromCode(type.code()));
    }
  }

  @Test
  void mqttSn2ConnectAndPingUseTheirOwnControlIdentifiers() {
    assertEquals(0x01, MqttSn2PacketType.CONNECT.code());
    assertEquals(0x0C, MqttSn2PacketType.PINGREQ.code());
    assertEquals(0x0F, MqttSn2PacketType.AUTH.code());
    assertNotEquals(0x04, MqttSn2PacketType.CONNECT.code());
    assertNotEquals(0x16, MqttSn2PacketType.PINGREQ.code());
  }

  @Test
  void encapsulationIdentifiersUseUnsignedOctets() {
    assertEquals(MqttSn2PacketType.FORWARDER_ENCAPSULATION, MqttSn2PacketType.fromCode(0xFC));
    assertEquals(MqttSn2PacketType.CONNECTION_ENCAPSULATION, MqttSn2PacketType.fromCode(0xFE));
    assertEquals(MqttSn2PacketType.PROTECTION_ENCAPSULATION, MqttSn2PacketType.fromCode(0xFF));
  }

  @Test
  void reservedAndOutOfRangeIdentifiersAreRejected() {
    for (int type : new int[]{0, 25, 251, 253, -1, 256}) {
      assertThrows(IllegalArgumentException.class, () -> MqttSn2PacketType.fromCode(type));
    }
  }
}
