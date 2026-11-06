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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocol;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NatsArchiveConnectionTest {

  @Test
  void archive_connection_keeps_identity_metadata_without_retaining_the_client_transport() {
    NatsProtocol protocol = mock(NatsProtocol.class);
    EndPoint endpoint = mock(EndPoint.class);
    when(protocol.getEndPoint()).thenReturn(endpoint);
    when(protocol.getName()).thenReturn("NATS");
    when(protocol.getVersion()).thenReturn("1.0");
    when(endpoint.getName()).thenReturn("nats-listener");
    when(endpoint.getProtocol()).thenReturn("nats");
    when(endpoint.getAuthenticationConfig()).thenReturn("maps-auth");
    NatsArchiveConnection connection = new NatsArchiveConnection(protocol);
    clearInvocations(protocol, endpoint);

    assertEquals(0, connection.getTimeOut());
    assertEquals("NATS", connection.getName());
    assertEquals("1.0", connection.getVersion());
    assertEquals("nats-listener", connection.getUniqueName());
    assertEquals("nats", connection.getProtocolName());
    assertEquals("maps-auth", connection.getAuthenticationConfig());
    connection.sendKeepAlive();
    verifyNoInteractions(protocol, endpoint);
  }
}
