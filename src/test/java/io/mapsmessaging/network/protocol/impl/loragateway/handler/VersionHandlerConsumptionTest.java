/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
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

package io.mapsmessaging.network.protocol.impl.loragateway.handler;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.loragateway.LoRaProtocol;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static io.mapsmessaging.network.protocol.impl.loragateway.Constants.CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class VersionHandlerConsumptionTest {

  @Test
  void consumes_version_before_sending_configuration_and_leaves_next_byte() throws Exception {
    LoRaProtocol protocol = mock(LoRaProtocol.class);
    byte[] config = {1, 2, 3};
    when(protocol.getConfigBuffer()).thenReturn(config);
    Packet packet = new Packet(ByteBuffer.wrap(new byte[]{4, 0x55}));
    assertTrue(new VersionHandler().processPacket(protocol, packet, 1, mock(Logger.class)));
    assertEquals(1, packet.position());
    assertEquals((byte) 0x55, packet.get());
    InOrder order = inOrder(protocol);
    order.verify(protocol).setSentVersion(true);
    order.verify(protocol).getConfigBuffer();
    order.verify(protocol).sendCommand(CONFIG, (byte) config.length, config);
    order.verify(protocol).setSentConfig(true);
  }

  @Test
  void missing_version_still_fails_before_sending_configuration() {
    LoRaProtocol protocol = mock(LoRaProtocol.class);
    Packet packet = new Packet(ByteBuffer.allocate(0));
    assertThrows(java.nio.BufferUnderflowException.class,
        () -> new VersionHandler().processPacket(protocol, packet, 1, mock(Logger.class)));
    verify(protocol).setSentVersion(true);
    verifyNoMoreInteractions(protocol);
  }
}
