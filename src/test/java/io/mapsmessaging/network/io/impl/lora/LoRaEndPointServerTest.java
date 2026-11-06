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

package io.mapsmessaging.network.io.impl.lora;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.loragateway.LoRaProtocol;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoRaEndPointServerTest {
  @Test
  void unbind_without_a_protocol_is_idempotent() {
    LoRaEndPointServer server = mock(LoRaEndPointServer.class, CALLS_REAL_METHODS);
    assertDoesNotThrow(() -> server.unbind(null));
  }

  @Test
  void unbind_closes_and_clears_the_bound_protocol() throws Exception {
    LoRaEndPointServer server = mock(LoRaEndPointServer.class, CALLS_REAL_METHODS);
    LoRaProtocol protocol = mock(LoRaProtocol.class);
    EndPoint endpoint = mock(EndPoint.class);
    when(protocol.getEndPoint()).thenReturn(endpoint);
    Field field = LoRaEndPointServer.class.getDeclaredField("loRaProtocol");
    field.setAccessible(true);
    field.set(server, protocol);

    server.unbind(null);

    verify(endpoint).close();
    assertNull(field.get(server));
    server.unbind(null);
    verify(endpoint, times(1)).close();
  }
}
