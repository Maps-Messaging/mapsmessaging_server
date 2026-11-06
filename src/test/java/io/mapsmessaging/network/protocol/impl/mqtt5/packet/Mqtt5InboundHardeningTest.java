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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.conformance.common.MqttWireClient;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessagePropertyFactory;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.ReceiveMaximum;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.SubscriptionIdentifier;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Mqtt5InboundHardeningTest {

  @Test
  void rejectsZeroReceiveMaximum() {
    assertThrows(MalformedException.class,
        () -> parseConnect(new byte[] {0x21, 0, 0}));
  }

  @Test
  void rejectsDuplicateReceiveMaximum() {
    assertThrows(MalformedException.class,
        () -> parseConnect(new byte[] {0x21, 0, 1, 0x21, 0, 2}));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 15, 65_535})
  void acceptsValidSingleReceiveMaximum(int maximum) throws Exception {
    Connect5 connect = parseConnect(new byte[] {
        0x21, (byte) (maximum >>> 8), (byte) maximum
    });

    ReceiveMaximum property = (ReceiveMaximum)
        connect.getProperties().get(MessagePropertyFactory.RECEIVE_MAXIMUM);
    assertNotNull(property);
    assertEquals(maximum, property.getReceiveMaximum());
  }

  @Test
  void absentReceiveMaximumRemainsValid() throws Exception {
    Connect5 connect = parseConnect(new byte[0]);
    assertNull(connect.getProperties().get(MessagePropertyFactory.RECEIVE_MAXIMUM));
  }

  @Test
  void outboundPublishCanCarrySubscriptionIdentifier() {
    Publish5 outgoing = new Publish5(new byte[] {1}, QualityOfService.AT_LEAST_ONCE,
        22, "topic/test", false);
    outgoing.add(new SubscriptionIdentifier(8));

    assertNotNull(outgoing.getProperties().get(MessagePropertyFactory.SUBSCRIPTION_IDENTIFIER));
    assertTrue(outgoing.getProperties().values().stream()
        .anyMatch(property -> property.getId() == MessagePropertyFactory.SUBSCRIPTION_IDENTIFIER));
  }

  private static Connect5 parseConnect(byte[] properties) throws Exception {
    byte[] wire = MqttWireClient.connect5("rx-validation", true, 30, properties);
    Packet packet = new Packet(ByteBuffer.wrap(wire));
    byte fixedHeader = packet.get();
    long remaining = MQTTPacket.readVariableInt(packet);
    return new Connect5(fixedHeader, remaining, packet);
  }
}
