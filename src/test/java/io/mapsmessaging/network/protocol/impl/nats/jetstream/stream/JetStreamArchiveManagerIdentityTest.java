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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JetStreamArchiveManagerIdentityTest {

  @Test
  void archiveIdentityIsStableAndIsolatedByUserAndStream() {
    String orders = JetStreamArchiveManager.archiveSessionId("alice", "ORDERS");
    assertEquals(orders, JetStreamArchiveManager.archiveSessionId("alice", "ORDERS"));
    assertNotEquals(orders, JetStreamArchiveManager.archiveSessionId("bob", "ORDERS"));
    assertNotEquals(orders, JetStreamArchiveManager.archiveSessionId("alice", "ALERTS"));
    assertTrue(orders.startsWith("nats-jetstream-archive-"));
    assertFalse(orders.contains("alice"));
  }
  @Test
  void archive_rejects_a_connection_without_authenticated_identity() {
    var connection = mock(io.mapsmessaging.network.protocol.impl.nats.state.SessionState.class);
    var session = mock(io.mapsmessaging.api.Session.class);
    var security = mock(io.mapsmessaging.engine.session.security.SecurityContext.class);
    when(connection.getSession()).thenReturn(session);
    when(session.getSecurityContext()).thenReturn(security);
    assertThrows(java.io.IOException.class,
        () -> JetStreamArchiveManager.getInstance().ensureArchive(connection, "AUTH_CHECK", "AUTH_CHECK/events"));
    when(security.isLoggedIn()).thenReturn(true);
    assertThrows(java.io.IOException.class,
        () -> JetStreamArchiveManager.getInstance().ensureArchive(connection, "AUTH_CHECK", "AUTH_CHECK/events"));
  }

}
