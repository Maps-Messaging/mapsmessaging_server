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

package io.mapsmessaging.network.protocol.impl.mqtt.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.conformance.common.MqttWireClient;
import io.mapsmessaging.network.protocol.impl.mqtt.MQTTProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.ConnAck;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Connect;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MqttUnsupportedLevelConformanceRegressionTest {

  @ParameterizedTest
  @CsvSource({"MQTT,4,true", "MQIsdp,3,true", "MQTT,6,false", "MQIsdp,4,false"})
  void protocolNameAndLevelMustAgree(String name, int level, boolean supported)
      throws Exception {
    Connect parsed = parse(name, level);
    assertEquals(level, parsed.getProtocolLevel());
    assertEquals(supported, parsed.hasSupportedProtocolLevel());
  }

  @Test
  void unsupportedLevelProducesConnAckOneAndClosesAfterWrite() throws Exception {
    Connect invalid = parse("MQTT", 6);
    EndPoint endpoint = mock(EndPoint.class);
    MQTTProtocol protocol = mock(MQTTProtocol.class);

    ConnAck response = (ConnAck) new ConnectListener()
        .handlePacket(invalid, null, endpoint, protocol);

    assertNotNull(response);
    assertEquals(ConnAck.INVALID_PROTOCOL, response.getResponseCode());
    assertNotNull(response.getCallback());
    response.complete();
    verify(endpoint).close();
  }

  private static Connect parse(String protocolName, int protocolLevel) throws Exception {
    byte[] wire = MqttWireClient.connectPacket(
        protocolName, protocolLevel, 0x02, 30, new byte[0], "test-id");
    Packet packet = new Packet(ByteBuffer.wrap(wire));
    byte fixed = packet.get();
    long remaining = MQTTPacket.readVariableInt(packet);
    return new Connect(fixed, remaining, packet);
  }
}
